package desarrolloWeb.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/* Cálculo de la planilla mensual (régimen general, Perú).
   Tasas referenciales: la SBS publica las comisiones de AFP y pueden cambiar; actualizar aquí cuando ocurra.
   UIT, remuneración mínima y remuneración máxima asegurable llegan en Parametros (application.properties). */
public final class CalculadoraPlanilla {

    public static final BigDecimal TASA_ONP = new BigDecimal("0.1300");
    public static final BigDecimal APORTE_AFP = new BigDecimal("0.1000");
    public static final BigDecimal PRIMA_SEGURO_AFP = new BigDecimal("0.0137");
    public static final BigDecimal TASA_ESSALUD = new BigDecimal("0.0900");
    /* Comisión sobre la remuneración (flujo) de cada AFP */
    public static final Map<String, BigDecimal> COMISION_AFP = Map.of(
            "Habitat", new BigDecimal("0.0147"),
            "Integra", new BigDecimal("0.0155"),
            "Prima", new BigDecimal("0.0160"),
            "Profuturo", new BigDecimal("0.0169"));

    /* El mes laboral se cuenta como 30 días */
    public static final int DIAS_MES = 30;

    /* Renta de 5ta: 7 UIT de deducción y escala progresiva acumulativa (límite del tramo en UIT → tasa) */
    static final BigDecimal DEDUCCION_UIT = new BigDecimal("7");
    private static final BigDecimal[][] TRAMOS_RENTA = {
            {new BigDecimal("5"), new BigDecimal("0.08")},
            {new BigDecimal("20"), new BigDecimal("0.14")},
            {new BigDecimal("35"), new BigDecimal("0.17")},
            {new BigDecimal("45"), new BigDecimal("0.20")},
            {null, new BigDecimal("0.30")}};
    /* Gratificaciones ordinarias de julio y diciembre: entran en la proyección anual */
    private static final int GRATIFICACIONES_ANUALES = 2;

    private CalculadoraPlanilla() {
    }

    /* uit: Unidad Impositiva Tributaria del año · rmv: remuneración mínima vital (base mínima de EsSalud)
       · rma: remuneración máxima asegurable de la SBS (tope de la prima de seguro AFP, cambia cada trimestre) */
    public record Parametros(BigDecimal uit, BigDecimal rmv, BigDecimal rma) {
        public static final Parametros REFERENCIALES =
                new Parametros(new BigDecimal("5500.00"), new BigDecimal("1130.00"), new BigDecimal("12234.34"));
    }

    public record Calculo(int diasLaborados, BigDecimal remuneracionBruta, BigDecimal tasaPension,
                          BigDecimal descuentoPension, BigDecimal essalud, BigDecimal rentaQuinta, BigDecimal neto) {

        /* Mismo cálculo con la retención de 5ta descontada del neto */
        public Calculo conRentaQuinta(BigDecimal renta) {
            return new Calculo(diasLaborados, remuneracionBruta, tasaPension, descuentoPension, essalud, renta,
                    remuneracionBruta.subtract(descuentoPension).subtract(renta));
        }
    }

    /* Días trabajados en el periodo según ingreso y cese; 0 si no estuvo en planilla ese mes */
    public static int diasLaborados(LocalDate ingreso, LocalDate cese, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        if (ingreso == null || ingreso.isAfter(fin) || (cese != null && cese.isBefore(inicio))) {
            return 0;
        }
        boolean mesCompleto = !ingreso.isAfter(inicio) && (cese == null || !cese.isBefore(fin));
        if (mesCompleto) {
            return DIAS_MES;
        }
        LocalDate desde = ingreso.isAfter(inicio) ? ingreso : inicio;
        LocalDate hasta = cese != null && cese.isBefore(fin) ? cese : fin;
        int dias = (int) (hasta.toEpochDay() - desde.toEpochDay()) + 1;
        return Math.min(DIAS_MES, dias);
    }

    public static BigDecimal tasaPension(String sistema, String afp) {
        if ("ONP".equals(sistema)) {
            return TASA_ONP;
        }
        if ("AFP".equals(sistema)) {
            return APORTE_AFP.add(PRIMA_SEGURO_AFP).add(comisionAfp(afp));
        }
        throw new IllegalArgumentException("Sistema de pensiones no registrado");
    }

    public static Calculo calcular(BigDecimal sueldo, int diasLaborados, String sistema, String afp) {
        return calcular(sueldo, diasLaborados, sistema, afp, Parametros.REFERENCIALES);
    }

    /* Remuneración, pensión y EsSalud del mes. La renta de 5ta se agrega aparte porque depende del resto del año. */
    public static Calculo calcular(BigDecimal sueldo, int diasLaborados, String sistema, String afp, Parametros p) {
        BigDecimal bruto = proporcional(sueldo, diasLaborados);
        BigDecimal tasa = tasaPension(sistema, afp);

        BigDecimal pension;
        if ("AFP".equals(sistema)) {
            // La prima de seguro solo se cobra hasta la remuneración máxima asegurable
            BigDecimal baseSeguro = bruto.min(p.rma());
            pension = bruto.multiply(APORTE_AFP.add(comisionAfp(afp)))
                    .add(baseSeguro.multiply(PRIMA_SEGURO_AFP))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            pension = bruto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        }

        // EsSalud no se calcula sobre menos que la remuneración mínima (proporcional a los días del mes)
        BigDecimal baseEssalud = bruto.max(proporcional(p.rmv(), diasLaborados));
        BigDecimal essalud = baseEssalud.multiply(TASA_ESSALUD).setScale(2, RoundingMode.HALF_UP);

        return new Calculo(diasLaborados, bruto, tasa, pension, essalud, BigDecimal.ZERO.setScale(2),
                bruto.subtract(pension));
    }

    /* Retención de renta de 5ta categoría del mes (procedimiento del art. 40 del Reglamento de la Ley del IR):
       1. Se proyecta el ingreso del año: lo percibido en meses anteriores + lo de este mes
          + el sueldo de los meses que faltan + las gratificaciones de julio y diciembre.
       2. Se restan 7 UIT y se aplica la escala (8 %, 14 %, 17 %, 20 % y 30 %).
       3. Al impuesto anual se le restan las retenciones de los meses indicados y se divide entre el número que
          corresponde al mes: ene-mar 12, abr 9, may-jul 8, ago 5, set-nov 4, dic lo que falte.
       retenidoPorMes: retención de cada mes anterior del mismo año (mes → monto). */
    public static BigDecimal retencionQuinta(int mes, BigDecimal remuneracionMes, BigDecimal sueldoMensual,
                                             BigDecimal percibidoMesesAnteriores, Map<Integer, BigDecimal> retenidoPorMes,
                                             Parametros p) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mes inválido: " + mes);
        }
        BigDecimal proyectada = percibidoMesesAnteriores
                .add(remuneracionMes)
                .add(sueldoMensual.multiply(BigDecimal.valueOf(12 - mes)))
                .add(sueldoMensual.multiply(BigDecimal.valueOf(GRATIFICACIONES_ANUALES)));
        BigDecimal impuestoAnual = impuestoAnual(proyectada, p.uit());

        int hastaMes;
        int divisor;
        if (mes <= 3) {
            hastaMes = 0;
            divisor = 12;
        } else if (mes == 4) {
            hastaMes = 3;
            divisor = 9;
        } else if (mes <= 7) {
            hastaMes = 4;
            divisor = 8;
        } else if (mes == 8) {
            hastaMes = 7;
            divisor = 5;
        } else if (mes <= 11) {
            hastaMes = 8;
            divisor = 4;
        } else {
            hastaMes = 11;
            divisor = 1;
        }
        BigDecimal retenido = retenidoPorMes.entrySet().stream()
                .filter(e -> e.getKey() <= hastaMes)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retencion = impuestoAnual.subtract(retenido)
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        return retencion.signum() > 0 ? retencion : BigDecimal.ZERO.setScale(2);
    }

    /* Impuesto anual sobre la renta proyectada, después de deducir 7 UIT */
    public static BigDecimal impuestoAnual(BigDecimal rentaBrutaAnual, BigDecimal uit) {
        BigDecimal neta = rentaBrutaAnual.subtract(uit.multiply(DEDUCCION_UIT));
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal limiteAnterior = BigDecimal.ZERO;
        for (BigDecimal[] tramo : TRAMOS_RENTA) {
            if (neta.compareTo(limiteAnterior) <= 0) {
                break;
            }
            BigDecimal limite = tramo[0] == null ? null : uit.multiply(tramo[0]);
            BigDecimal tope = limite == null ? neta : neta.min(limite);
            impuesto = impuesto.add(tope.subtract(limiteAnterior).multiply(tramo[1]));
            if (limite == null) {
                break;
            }
            limiteAnterior = limite;
        }
        return impuesto.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal proporcional(BigDecimal mensual, int dias) {
        return dias >= DIAS_MES
                ? mensual.setScale(2, RoundingMode.HALF_UP)
                : mensual.multiply(BigDecimal.valueOf(dias)).divide(BigDecimal.valueOf(DIAS_MES), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal comisionAfp(String afp) {
        BigDecimal comision = COMISION_AFP.get(afp);
        if (comision == null) {
            throw new IllegalArgumentException("AFP desconocida: " + afp);
        }
        return comision;
    }
}
