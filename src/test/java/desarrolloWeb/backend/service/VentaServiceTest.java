package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.TicketPromedioResponse;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.dto.VentasPorComprobante;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService")
class VentaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 8, 15);
    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ConfiguracionTributariaService configuracionService;

    @Mock
    private FacturacionService facturacionService;

    @InjectMocks
    private VentaService ventaService;

    private Cliente clienteDni;
    private Cliente clienteRuc;

    @BeforeEach
    void setUp() {
        clienteDni = new Cliente("10203040", "Carlos Mendoza", TipoDocumento.DNI);
        clienteDni.setId(1L);
        clienteRuc = new Cliente("20555666777", "Comercial Lima S.A.C.", TipoDocumento.RUC);
        clienteRuc.setId(2L);
    }

    @Test
    @DisplayName("un cliente con DNI recibe BOLETA y S/ 300.00 genera IGV 54.00")
    void registrarBoletaParaDni() {
        when(clienteRepository.findByDocumento("10203040")).thenReturn(Optional.of(clienteDni));
        when(ventaRepository.existsByNumeroComprobante("B001-1")).thenReturn(false);
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta venta = inv.getArgument(0);
            venta.setId(7L);
            return venta;
        });

        VentaResponse respuesta = ventaService.registrar(
                new VentaRequest("10203040", "B001-1", new BigDecimal("300.00"), FECHA));

        assertAll(
                () -> assertEquals(TipoComprobante.BOLETA, respuesta.tipoComprobante()),
                () -> assertEquals(new BigDecimal("54.00"), respuesta.igv()),
                () -> assertEquals(new BigDecimal("354.00"), respuesta.montoTotal()),
                () -> assertEquals(EstadoVenta.EMITIDA, respuesta.estado()),
                () -> assertEquals(1L, respuesta.clienteId())
        );
    }

    @Test
    @DisplayName("un cliente con RUC y comprobante B lanza DocumentoInvalidoException (400)")
    void rucConPrefijoBoleta() {
        when(clienteRepository.findByDocumento("20555666777")).thenReturn(Optional.of(clienteRuc));

        DocumentoInvalidoException error = assertThrows(DocumentoInvalidoException.class,
                () -> ventaService.registrar(new VentaRequest("20555666777", "B001-1", BigDecimal.TEN, FECHA)));

        assertEquals("El cliente con RUC requiere FACTURA: el comprobante debe empezar con 'F'", error.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un número de comprobante repetido lanza 409")
    void comprobanteDuplicado() {
        when(clienteRepository.findByDocumento("20555666777")).thenReturn(Optional.of(clienteRuc));
        when(ventaRepository.existsByNumeroComprobante("F001-1")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> ventaService.registrar(new VentaRequest("20555666777", "F001-1", BigDecimal.TEN, FECHA)));

        assertEquals("Ya existe una venta con el comprobante F001-1", error.getMessage());
    }

    @Test
    @DisplayName("un cliente inexistente lanza 404")
    void clienteInexistente() {
        when(clienteRepository.findByDocumento("99999999")).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> ventaService.registrar(new VentaRequest("99999999", "B001-1", BigDecimal.TEN, FECHA)));

        assertEquals("No existe un cliente con documento 99999999", error.getMessage());
    }

    @Test
    @DisplayName("anular cambia el estado a ANULADA sin llamar a save")
    void anularVentaEmitida() {
        Venta venta = venta(EstadoVenta.EMITIDA);
        when(ventaRepository.findById(7L)).thenReturn(Optional.of(venta));

        VentaResponse respuesta = ventaService.anular(7L);

        assertEquals(EstadoVenta.ANULADA, respuesta.estado());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("anular dos veces la misma venta lanza 409")
    void anularDosVeces() {
        when(ventaRepository.findById(7L)).thenReturn(Optional.of(venta(EstadoVenta.ANULADA)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> ventaService.anular(7L));

        assertEquals("La venta B001-1 ya está anulada", error.getMessage());
    }

    @Test
    @DisplayName("el ticket promedio convierte el Double de AVG a BigDecimal con 2 decimales")
    void ticketPromedio() {
        when(ventaRepository.promedioMontoSinIgv(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(233.3333);

        TicketPromedioResponse respuesta = ventaService.calcularTicketPromedio(DESDE, HASTA);

        assertEquals(new BigDecimal("233.33"), respuesta.ticketPromedio());
    }

    @Test
    @DisplayName("sin ventas en el periodo el ticket promedio es 0.00")
    void ticketPromedioSinVentas() {
        when(ventaRepository.promedioMontoSinIgv(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(null);

        assertEquals(new BigDecimal("0.00"), ventaService.calcularTicketPromedio(DESDE, HASTA).ticketPromedio());
    }

    @Test
    @DisplayName("el reporte cuenta ventas EMITIDAS por cada tipo de comprobante")
    void reportePorComprobante() {
        when(ventaRepository.contarPorTipoComprobante(TipoComprobante.FACTURA, EstadoVenta.EMITIDA)).thenReturn(2L);
        when(ventaRepository.contarPorTipoComprobante(TipoComprobante.BOLETA, EstadoVenta.EMITIDA)).thenReturn(3L);

        List<VentasPorComprobante> reporte = ventaService.contarPorComprobante();

        assertAll(
                () -> assertEquals(2, reporte.size()),
                () -> assertEquals(new VentasPorComprobante(TipoComprobante.FACTURA, 2L), reporte.get(0)),
                () -> assertEquals(new VentasPorComprobante(TipoComprobante.BOLETA, 3L), reporte.get(1))
        );
    }

    @Test
    @DisplayName("el IGV del periodo suma solo las ventas EMITIDAS")
    void igvDelPeriodo() {
        Venta v1 = venta(EstadoVenta.EMITIDA);
        v1.setIgv(new BigDecimal("54.00"));
        Venta v2 = venta(EstadoVenta.EMITIDA);
        v2.setIgv(new BigDecimal("36.00"));
        when(ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(List.of(v1, v2));

        assertEquals(new BigDecimal("90.00"), ventaService.calcularTotalIgvPeriodo(DESDE, HASTA));
    }

    private Venta venta(EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setId(7L);
        venta.setCliente(clienteDni);
        venta.setTipoComprobante(TipoComprobante.BOLETA);
        venta.setNumeroComprobante("B001-1");
        venta.setEstado(estado);
        return venta;
    }
}
