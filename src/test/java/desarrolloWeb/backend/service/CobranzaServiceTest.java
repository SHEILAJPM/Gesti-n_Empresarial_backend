package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CobranzaDtos.CobroRequest;
import desarrolloWeb.backend.dto.CobranzaDtos.EstadoCuenta;
import desarrolloWeb.backend.dto.CobranzaDtos.EstadoDeCuenta;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.Cobro;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.FormaPago;
import desarrolloWeb.backend.entity.MedioPago;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaCuota;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.CobroRepository;
import desarrolloWeb.backend.repository.NotaRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cuentas por cobrar")
class CobranzaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 20);

    @Mock private VentaRepository ventaRepository;
    @Mock private CobroRepository cobroRepository;
    @Mock private NotaRepository notaRepository;

    private CobranzaService servicio;
    private Venta venta;
    private final List<Cobro> cobros = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(ZonedDateTime.of(HOY.atTime(9, 0), PortalService.ZONA).toInstant(), PortalService.ZONA);
        servicio = new CobranzaService(ventaRepository, cobroRepository, notaRepository, reloj);

        venta = new Venta();
        venta.setId(3L);
        Cliente cliente = new Cliente("20601234562", "Inversiones Pacífico E.I.R.L.", TipoDocumento.RUC);
        cliente.setId(9L);
        venta.setCliente(cliente);
        venta.setTipoComprobante(TipoComprobante.FACTURA);
        venta.setNumeroComprobante("F001-3");
        venta.setFecha(LocalDate.of(2026, 11, 5));
        venta.setMontoTotal(new BigDecimal("1000.00"));
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setFormaPago(FormaPago.CREDITO);
        venta.agregarCuota(cuota("2026-12-05", "500.00"));
        venta.agregarCuota(cuota("2027-01-05", "500.00"));

        lenient().when(ventaRepository.findById(3L)).thenReturn(Optional.of(venta));
        lenient().when(notaRepository.findByVentaIdOrderByIdAsc(3L)).thenReturn(List.of());
        lenient().when(cobroRepository.findByVentaIdOrderByFechaAscIdAsc(3L)).thenAnswer(i -> List.copyOf(cobros));
        lenient().when(cobroRepository.save(any(Cobro.class))).thenAnswer(i -> {
            cobros.add(i.getArgument(0));
            return i.getArgument(0);
        });
    }

    private static VentaCuota cuota(String vence, String monto) {
        VentaCuota c = new VentaCuota();
        c.setFechaVencimiento(LocalDate.parse(vence));
        c.setMonto(new BigDecimal(monto));
        return c;
    }

    @Test
    void laPrimeraCuotaSinPagarEstaVencidaYElCobroLaCubre() {
        EstadoDeCuenta antes = servicio.estadoDeCuenta(3L);
        assertEquals(EstadoCuenta.VENCIDA, antes.cuenta().estado());
        assertEquals(15, antes.cuenta().diasAtraso());

        EstadoDeCuenta despues = servicio.registrarCobro(new CobroRequest(3L, new BigDecimal("500.00"), null,
                MedioPago.TRANSFERENCIA, "OP-778812"), "contabilidad1");

        assertEquals(EstadoCuenta.AL_DIA, despues.cuenta().estado());
        assertEquals(new BigDecimal("500.00"), despues.cuenta().saldo());
        assertEquals(1, despues.cobros().size());
        assertEquals("OP-778812", despues.cobros().get(0).referencia());
    }

    @Test
    void unCobroNoPuedeSuperarElSaldo() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.registrarCobro(
                new CobroRequest(3L, new BigDecimal("1200.00"), null, MedioPago.EFECTIVO, null), "contabilidad1"));
        assertTrue(e.getMessage().contains("supera el saldo"));
        verify(cobroRepository, never()).save(any());
    }

    @Test
    void lasVentasAlContadoNoTienenCuentaPorCobrar() {
        venta.setFormaPago(FormaPago.CONTADO);
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCobro(
                new CobroRequest(3L, BigDecimal.TEN, null, MedioPago.EFECTIVO, null), "contabilidad1"));
    }

    @Test
    void conTodoCobradoQuedaPagadaYNoAceptaMas() {
        servicio.registrarCobro(new CobroRequest(3L, new BigDecimal("1000.00"), null, MedioPago.DEPOSITO, null), "c");
        assertEquals(EstadoCuenta.PAGADA, servicio.estadoDeCuenta(3L).cuenta().estado());
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCobro(
                new CobroRequest(3L, BigDecimal.ONE, null, MedioPago.EFECTIVO, null), "c"));
    }
}
