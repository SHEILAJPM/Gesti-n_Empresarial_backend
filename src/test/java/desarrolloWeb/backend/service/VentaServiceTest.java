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

    @Mock
    private desarrolloWeb.backend.repository.ProductoRepository productoRepository;

    @InjectMocks
    private VentaService ventaService;

    private void prepararFactura(String numero) {
        when(clienteRepository.findByDocumento("20555666777")).thenReturn(Optional.of(clienteRuc));
        when(ventaRepository.existsByNumeroComprobante(numero)).thenReturn(false);
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));
        org.mockito.Mockito.lenient().when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static VentaRequest.Item item(String descripcion, String unidad, String cantidad, String precio) {
        return new VentaRequest.Item(null, null, descripcion, unidad, new BigDecimal(cantidad), new BigDecimal(precio));
    }

    @Test
    @DisplayName("una factura con varias líneas suma cada cantidad × precio y calcula el IGV")
    void facturaConDetalle() {
        prepararFactura("F001-5");
        VentaRequest pedido = new VentaRequest("20555666777", "F001-5", null, FECHA,
                List.of(item("Laptop Lenovo 14\"", "NIU", "2", "1500.00"), item("Instalación de software", "ZZ", "1", "120.50")),
                null, null, "Entrega en tienda");

        VentaResponse venta = ventaService.registrar(pedido);

        assertEquals(new BigDecimal("3120.50"), venta.montoSinIgv());
        assertEquals(new BigDecimal("561.69"), venta.igv());
        assertEquals(new BigDecimal("3682.19"), venta.montoTotal());
        assertEquals(2, venta.items().size());
        assertEquals(new BigDecimal("3000.00"), venta.items().get(0).valorVenta());
        assertEquals(new BigDecimal("540.00"), venta.items().get(0).igv());
        assertEquals("ZZ", venta.items().get(1).unidad());
        assertEquals("Entrega en tienda", venta.observaciones());
        assertEquals(desarrolloWeb.backend.entity.FormaPago.CONTADO, venta.formaPago());
    }

    @Test
    @DisplayName("al crédito las cuotas deben vencer después de la emisión y sumar el total")
    void facturaAlCredito() {
        prepararFactura("F001-6");
        List<VentaRequest.Item> items = List.of(item("Mercadería", "NIU", "10", "100.00")); // total 1180.00

        VentaRequest malSumado = new VentaRequest("20555666777", "F001-6", null, FECHA, items,
                desarrolloWeb.backend.entity.FormaPago.CREDITO,
                List.of(new VentaRequest.Cuota(FECHA.plusDays(30), new BigDecimal("500.00"))), null);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ventaService.registrar(malSumado));
        assertTrue(error.getMessage().contains("deben sumar el total de S/ 1180.00"), error.getMessage());

        VentaRequest bien = new VentaRequest("20555666777", "F001-6", null, FECHA, items,
                desarrolloWeb.backend.entity.FormaPago.CREDITO,
                List.of(new VentaRequest.Cuota(FECHA.plusDays(30), new BigDecimal("590.00")),
                        new VentaRequest.Cuota(FECHA.plusDays(60), new BigDecimal("590.00"))), null);
        VentaResponse venta = ventaService.registrar(bien);
        assertEquals(2, venta.cuotas().size());
        assertEquals(2, venta.cuotas().get(1).numero());
    }

    @Test
    @DisplayName("una cuota que vence el mismo día de la emisión no es válida")
    void cuotaSinPlazo() {
        prepararFactura("F001-7");
        VentaRequest pedido = new VentaRequest("20555666777", "F001-7", null, FECHA,
                List.of(item("Mercadería", "NIU", "1", "100.00")), desarrolloWeb.backend.entity.FormaPago.CREDITO,
                List.of(new VentaRequest.Cuota(FECHA, new BigDecimal("118.00"))), null);

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrar(pedido));
    }

    @Test
    @DisplayName("sin líneas ni monto no se puede emitir")
    void sinDetalleNiMonto() {
        when(clienteRepository.findByDocumento("20555666777")).thenReturn(Optional.of(clienteRuc));
        when(ventaRepository.existsByNumeroComprobante("F001-8")).thenReturn(false);
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrar(
                new VentaRequest("20555666777", "F001-8", null, FECHA, List.of(), null, null, null)));
    }

    @Test
    @DisplayName("una unidad que no está en el catálogo de SUNAT se rechaza")
    void unidadInvalida() {
        when(clienteRepository.findByDocumento("20555666777")).thenReturn(Optional.of(clienteRuc));
        when(ventaRepository.existsByNumeroComprobante("F001-9")).thenReturn(false);
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrar(new VentaRequest("20555666777",
                "F001-9", null, FECHA, List.of(item("Algo", "XYZ", "1", "10.00")), null, null, null)));
    }

    @Test
    @DisplayName("las boletas no se emiten al crédito")
    void boletaAlCredito() {
        when(clienteRepository.findByDocumento("10203040")).thenReturn(Optional.of(clienteDni));
        when(ventaRepository.existsByNumeroComprobante("B001-9")).thenReturn(false);
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ventaService.registrar(
                new VentaRequest("10203040", "B001-9", null, FECHA, List.of(item("Algo", "NIU", "1", "10.00")),
                        desarrolloWeb.backend.entity.FormaPago.CREDITO,
                        List.of(new VentaRequest.Cuota(FECHA.plusDays(10), new BigDecimal("11.80"))), null)));
        assertTrue(error.getMessage().contains("factura"));
    }

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

        assertTrue(error.getMessage().startsWith("Ya existe una venta con el comprobante F001-1. El siguiente número libre es F001-"), error.getMessage());
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
