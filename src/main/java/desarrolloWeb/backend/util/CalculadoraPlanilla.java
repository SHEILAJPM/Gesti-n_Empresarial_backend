package desarrolloWeb.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/* Cálculo de la planilla mensual (régimen general, Perú).
   Tasas referenciales: la SBS publica las comisiones de AFP y pueden cambiar; actualizar aquí cuando ocurra. */
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

    private CalculadoraPlanilla() {
    }

    public record Calculo(int diasLaborados, BigDecimal remuneracionBruta, BigDecimal tasaPension,
                          BigDecimal descuentoPension, BigDecimal essalud, BigDecimal neto) {
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
            BigDecimal comision = COMISION_AFP.get(afp);
            if (comision == null) {
                throw new IllegalArgumentException("AFP desconocida: " + afp);
            }
            return APORTE_AFP.add(PRIMA_SEGURO_AFP).add(comision);
        }
        throw new IllegalArgumentException("Sistema de pensiones no registrado");
    }

    public static Calculo calcular(BigDecimal sueldo, int diasLaborados, String sistema, String afp) {
        BigDecimal bruto = diasLaborados >= DIAS_MES
                ? sueldo.setScale(2, RoundingMode.HALF_UP)
                : sueldo.multiply(BigDecimal.valueOf(diasLaborados))
                        .divide(BigDecimal.valueOf(DIAS_MES), 2, RoundingMode.HALF_UP);
        BigDecimal tasa = tasaPension(sistema, afp);
        BigDecimal pension = bruto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal essalud = bruto.multiply(TASA_ESSALUD).setScale(2, RoundingMode.HALF_UP);
        return new Calculo(diasLaborados, bruto, tasa, pension, essalud, bruto.subtract(pension));
    }
}
