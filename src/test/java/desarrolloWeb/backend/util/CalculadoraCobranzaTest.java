package desarrolloWeb.backend.util;

import desarrolloWeb.backend.util.CalculadoraCobranza.EstadoTramo;
import desarrolloWeb.backend.util.CalculadoraCobranza.Resultado;
import desarrolloWeb.backend.util.CalculadoraCobranza.Tramo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Cuentas por cobrar: aplicación de cobros y notas a las cuotas")
class CalculadoraCobranzaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    private static final List<Tramo> DOS_CUOTAS = List.of(
            new Tramo("Cuota 1", LocalDate.of(2026, 11, 10), new BigDecimal("500.00")),
            new Tramo("Cuota 2", LocalDate.of(2026, 12, 10), new BigDecimal("500.00")));

    @Test
    void sinCobrosLaCuotaPasadaEstaVencida() {
        Resultado r = CalculadoraCobranza.calcular(DOS_CUOTAS, BigDecimal.ZERO, BigDecimal.ZERO, HOY);

        assertEquals(EstadoTramo.VENCIDA, r.tramos().get(0).estado());
        assertEquals(10, r.tramos().get(0).diasVencido());
        assertEquals(EstadoTramo.PENDIENTE, r.tramos().get(1).estado());
        assertEquals(new BigDecimal("500.00"), r.vencido());
        assertEquals(new BigDecimal("1000.00"), r.saldo());
        assertEquals(LocalDate.of(2026, 11, 10), r.proximoVencimiento());
    }

    @Test
    void losCobrosCubrenPrimeroLaCuotaMasAntigua() {
        Resultado r = CalculadoraCobranza.calcular(DOS_CUOTAS, BigDecimal.ZERO, new BigDecimal("700.00"), HOY);

        assertEquals(EstadoTramo.PAGADA, r.tramos().get(0).estado());
        assertEquals(EstadoTramo.PARCIAL, r.tramos().get(1).estado());
        assertEquals(new BigDecimal("300.00"), r.tramos().get(1).pendiente());
        assertEquals(BigDecimal.ZERO, r.vencido());
        assertEquals(new BigDecimal("300.00"), r.saldo());
    }

    @Test
    void laNotaDeCreditoRebajaDesdeLaUltimaCuota() {
        Resultado r = CalculadoraCobranza.calcular(DOS_CUOTAS, new BigDecimal("200.00"), BigDecimal.ZERO, HOY);

        assertEquals(new BigDecimal("500.00"), r.tramos().get(0).monto());
        assertEquals(new BigDecimal("300.00"), r.tramos().get(1).monto());
        assertEquals(new BigDecimal("800.00"), r.deuda());
    }

    @Test
    void unaNotaDeCreditoTotalDejaLaCuentaSinDeuda() {
        Resultado r = CalculadoraCobranza.calcular(DOS_CUOTAS, new BigDecimal("1000.00"), BigDecimal.ZERO, HOY);

        assertEquals(0, r.saldo().signum());
        assertEquals(0, r.vencido().signum());
        assertEquals(0, r.tramos().size());
    }

    @Test
    void laNotaDeDebitoEsUnCargoMasQueSeCobraEnSuOrden() {
        List<Tramo> conInteres = List.of(DOS_CUOTAS.get(0),
                new Tramo("Nota de débito FD01-1", LocalDate.of(2026, 11, 15), new BigDecimal("59.00")),
                DOS_CUOTAS.get(1));
        Resultado r = CalculadoraCobranza.calcular(conInteres, BigDecimal.ZERO, new BigDecimal("559.00"), HOY);

        assertEquals(EstadoTramo.PAGADA, r.tramos().get(1).estado());
        assertEquals(new BigDecimal("500.00"), r.saldo());
    }
}
