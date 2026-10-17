package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.entity.Trabajador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PersonalService personalService;
    @Mock
    private ProveedorService proveedorService;

    private PagoService pagoService;
    private Trabajador trabajador;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(personalService, proveedorService);
        trabajador = new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", "ACTIVO");
        proveedor = new Proveedor("20123456789", "Distribuidora SAC", null);
        proveedor.setId(5L);
    }

    @Test
    void registrarConDocumentoInexistenteLanzaExcepcion() {
        when(personalService.buscarPorDocumento("99999999")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> pagoService.registrar("99999999", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31)));
    }

    @Test
    void registrarQuedaComoPendienteLigadoAlTrabajador() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));

        Pago pago = pagoService.registrar("70001234", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31));

        assertNotNull(pago.getId());
        assertEquals("TRABAJADOR", pago.getTipoBeneficiario());
        assertEquals(trabajador.getId(), pago.getTrabajadorId());
        assertNull(pago.getProveedorId());
        assertEquals("PENDIENTE", pago.getEstado());
        assertNull(pago.getFechaPago());
    }

    @Test
    void registrarProveedorConRucInexistenteLanzaExcepcion() {
        when(proveedorService.buscarPorRuc("99999999999")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> pagoService.registrarProveedor("99999999999", "Factura F001-1", 500.0, LocalDate.of(2026, 8, 31)));
    }

    @Test
    void registrarProveedorQuedaComoPendienteLigadoAlProveedor() {
        when(proveedorService.buscarPorRuc("20123456789")).thenReturn(Optional.of(proveedor));

        Pago pago = pagoService.registrarProveedor(
                "20123456789", "Factura F001-1", 500.0, LocalDate.of(2026, 8, 31));

        assertNotNull(pago.getId());
        assertEquals("PROVEEDOR", pago.getTipoBeneficiario());
        assertEquals(proveedor.getId(), pago.getProveedorId());
        assertNull(pago.getTrabajadorId());
        assertEquals("PENDIENTE", pago.getEstado());
        assertNull(pago.getFechaPago());
    }

    @Test
    void marcarComoPagadoActualizaEstadoYFecha() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        Pago pago = pagoService.registrar("70001234", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31));

        Pago pagado = pagoService.marcarComoPagado(pago.getId());

        assertEquals("PAGADO", pagado.getEstado());
        assertNotNull(pagado.getFechaPago());
    }

    @Test
    void marcarComoPagadoDosVecesLanzaExcepcion() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        Pago pago = pagoService.registrar("70001234", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31));
        pagoService.marcarComoPagado(pago.getId());

        assertThrows(IllegalStateException.class, () -> pagoService.marcarComoPagado(pago.getId()));
    }

    @Test
    void listarPendientesExcluyeLosYaPagados() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        Pago pendiente = pagoService.registrar("70001234", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31));
        Pago pagado = pagoService.registrar("70001234", "Sueldo julio", 1500.0, LocalDate.of(2026, 7, 31));
        pagoService.marcarComoPagado(pagado.getId());

        List<Pago> pendientes = pagoService.listarPendientes();

        assertEquals(1, pendientes.size());
        assertEquals(pendiente.getId(), pendientes.get(0).getId());
    }

    @Test
    void listarPorTrabajadorDevuelveSoloSusPagos() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        pagoService.registrar("70001234", "Sueldo agosto", 1500.0, LocalDate.of(2026, 8, 31));
        pagoService.registrar("70001234", "Sueldo julio", 1500.0, LocalDate.of(2026, 7, 31));

        List<Pago> pagos = pagoService.listarPorTrabajador(trabajador.getId());

        assertEquals(2, pagos.size());
    }
}
