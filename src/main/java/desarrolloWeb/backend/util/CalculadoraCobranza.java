package desarrolloWeb.backend.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/* Estado de la deuda de una venta al crédito.
   - Tramos: las cuotas pactadas y, aparte, cada nota de débito (vence el día que se emitió).
   - Las notas de crédito reducen lo que se debe empezando por las últimas cuotas.
   - Los cobros se aplican a la cuota más antigua primero. */
public final class CalculadoraCobranza {

    public enum EstadoTramo { PAGADA, PARCIAL, PENDIENTE, VENCIDA }

    public record Tramo(String descripcion, LocalDate vence, BigDecimal monto) {
    }

    public record TramoCalculado(String descripcion, LocalDate vence, BigDecimal monto, BigDecimal cobrado,
                                 BigDecimal pendiente, EstadoTramo estado, long diasVencido) {
    }

    public record Resultado(List<TramoCalculado> tramos, BigDecimal deuda, BigDecimal cobrado, BigDecimal saldo,
                            BigDecimal vencido, LocalDate proximoVencimiento, long diasMaximoAtraso) {
    }

    private CalculadoraCobranza() {
    }

    public static Resultado calcular(List<Tramo> tramos, BigDecimal notasCredito, BigDecimal cobrado, LocalDate hoy) {
        List<BigDecimal> montos = new ArrayList<>();
        tramos.forEach(t -> montos.add(t.monto()));

        // 1. Las notas de crédito rebajan desde la última cuota hacia atrás
        BigDecimal credito = notasCredito;
        for (int i = montos.size() - 1; i >= 0 && credito.signum() > 0; i--) {
            BigDecimal rebaja = montos.get(i).min(credito);
            montos.set(i, montos.get(i).subtract(rebaja));
            credito = credito.subtract(rebaja);
        }

        // 2. Los cobros cubren desde la primera cuota
        BigDecimal disponible = cobrado;
        List<TramoCalculado> resultado = new ArrayList<>();
        BigDecimal deuda = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        LocalDate proximo = null;
        long atraso = 0;
        for (int i = 0; i < tramos.size(); i++) {
            Tramo t = tramos.get(i);
            BigDecimal monto = montos.get(i);
            deuda = deuda.add(monto);
            BigDecimal aplicado = monto.min(disponible);
            disponible = disponible.subtract(aplicado);
            BigDecimal pendiente = monto.subtract(aplicado);

            EstadoTramo estado;
            long dias = 0;
            if (pendiente.signum() == 0) {
                estado = EstadoTramo.PAGADA;
            } else if (t.vence().isBefore(hoy)) {
                estado = EstadoTramo.VENCIDA;
                dias = ChronoUnit.DAYS.between(t.vence(), hoy);
                vencido = vencido.add(pendiente);
                atraso = Math.max(atraso, dias);
            } else {
                estado = aplicado.signum() > 0 ? EstadoTramo.PARCIAL : EstadoTramo.PENDIENTE;
            }
            if (pendiente.signum() > 0 && (proximo == null || t.vence().isBefore(proximo))) {
                proximo = t.vence();
            }
            if (monto.signum() > 0 || aplicado.signum() > 0) {
                resultado.add(new TramoCalculado(t.descripcion(), t.vence(), monto, aplicado, pendiente, estado, dias));
            }
        }
        BigDecimal saldo = deuda.subtract(cobrado).max(BigDecimal.ZERO);
        return new Resultado(resultado, deuda, cobrado, saldo, vencido, proximo, atraso);
    }
}
