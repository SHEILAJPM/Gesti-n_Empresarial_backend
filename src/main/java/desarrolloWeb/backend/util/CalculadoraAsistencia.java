package desarrolloWeb.backend.util;

import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/* Asistencia de un trabajador en un mes, a partir de sus marcaciones.
   - Días laborables: lunes a viernes entre su ingreso (o el día 1) y su cese, sin pasar de hoy. No descuenta feriados.
   - Tardanza: la primera ENTRADA del día llega después de hora de entrada + tolerancia;
     los minutos se cuentan desde la hora de entrada.
   - Horas: suma de los tramos ENTRADA → SALIDA. Un día que termina sin SALIDA no suma y se cuenta aparte.
   - Falta: día laborable sin ninguna marcación. */
public final class CalculadoraAsistencia {

    private CalculadoraAsistencia() {
    }

    public record Resultado(int diasLaborables, int diasAsistidos, int faltas, int tardanzas, int minutosTardanza,
                            BigDecimal horasTrabajadas, int diasSinSalida) {
    }

    public static Resultado calcular(List<Marcacion> marcaciones, YearMonth periodo, LocalDate ingreso,
                                     LocalDate cese, LocalDate hoy, LocalTime horaEntrada, int toleranciaMinutos) {
        Map<LocalDate, List<Marcacion>> porDia = marcaciones.stream()
                .sorted(Comparator.comparing(Marcacion::getFechaHora))
                .collect(Collectors.groupingBy(m -> m.getFechaHora().toLocalDate(), TreeMap::new, Collectors.toList()));

        LocalTime limite = horaEntrada.plusMinutes(toleranciaMinutos);
        int tardanzas = 0;
        int minutosTarde = 0;
        int sinSalida = 0;
        long minutosTrabajados = 0;

        for (Map.Entry<LocalDate, List<Marcacion>> dia : porDia.entrySet()) {
            List<Marcacion> lista = dia.getValue();
            Marcacion primeraEntrada = lista.stream().filter(m -> m.getTipo() == TipoMarcacion.ENTRADA)
                    .findFirst().orElse(null);
            if (primeraEntrada != null && primeraEntrada.getFechaHora().toLocalTime().isAfter(limite)) {
                tardanzas++;
                minutosTarde += (int) Duration.between(horaEntrada, primeraEntrada.getFechaHora().toLocalTime()).toMinutes();
            }

            LocalDateTime abierta = null;
            for (Marcacion m : lista) {
                if (m.getTipo() == TipoMarcacion.ENTRADA) {
                    abierta = m.getFechaHora();
                } else if (abierta != null) {
                    minutosTrabajados += Duration.between(abierta, m.getFechaHora()).toMinutes();
                    abierta = null;
                }
            }
            // Hoy todavía puede marcar la salida: no se cuenta como olvido
            if (abierta != null && dia.getKey().isBefore(hoy)) {
                sinSalida++;
            }
        }

        int laborables = 0;
        int faltas = 0;
        LocalDate desde = ingreso != null && ingreso.isAfter(periodo.atDay(1)) ? ingreso : periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        if (cese != null && cese.isBefore(hasta)) hasta = cese;
        if (hoy.isBefore(hasta)) hasta = hoy;
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            laborables++;
            // El día de hoy solo es falta cuando ya pasó la hora de salida; se deja fuera para no adelantarse
            if (!porDia.containsKey(d) && d.isBefore(hoy)) faltas++;
        }

        BigDecimal horas = BigDecimal.valueOf(minutosTrabajados).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        return new Resultado(laborables, porDia.size(), faltas, tardanzas, minutosTarde, horas, sinSalida);
    }
}
