package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.NotaRequest;
import desarrolloWeb.backend.dto.NotaResponse;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Nota;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.TipoNota;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.NotaRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.DocumentoUbl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notas de crédito y débito")
class NotaServiceTest {

    @Mock private NotaRepository notaRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private FacturacionService facturacionService;

    private NotaService notaService;
    private Venta venta;

    @BeforeEach
    void preparar() {
        notaService = new NotaService(notaRepository, ventaRepository, facturacionService);

        Cliente cliente = new Cliente("20555666777", "Comercial Lima S.A.C.", TipoDocumento.RUC);
        cliente.setId(2L);
        venta = new Venta();
        venta.setId(10L);
        venta.setCliente(cliente);
        venta.setTipoComprobante(TipoComprobante.FACTURA);
        venta.setNumeroComprobante("F001-25");
        venta.setFecha(LocalDate.now().minusDays(2));
        venta.setMontoSinIgv(new BigDecimal("3120.50"));
        venta.setIgv(new BigDecimal("561.69"));
        venta.setMontoTotal(new BigDecimal("3682.19"));
        venta.setTasaAplicada(new BigDecimal("0.1800"));
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.getSunat().setEstado(EstadoSunat.ACEPTADO);
        venta.agregarItem(item("LAP-01", "Laptop", "NIU", "2", "1500.00", "3000.00", "540.00"));
        venta.agregarItem(item(null, "Instalación", "ZZ", "1", "120.50", "120.50", "21.69"));

        lenient().when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));
        lenient().when(notaRepository.totalPorVentaYTipo(eq(10L), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(notaRepository.maximoCorrelativo(anyString(), anyInt())).thenReturn(null);
        lenient().when(facturacionService.siguienteCorrelativo(anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> inv.getArgument(2));
        lenient().when(notaRepository.save(any(Nota.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static VentaItem item(String codigo, String desc, String unidad, String cant, String vu, String vv, String igv) {
        VentaItem i = new VentaItem();
        i.setCodigo(codigo);
        i.setDescripcion(desc);
        i.setUnidad(unidad);
        i.setCantidad(new BigDecimal(cant));
        i.setValorUnitario(new BigDecimal(vu));
        i.setValorVenta(new BigDecimal(vv));
        i.setIgv(new BigDecimal(igv));
        return i;
    }

    private static NotaRequest pedido(TipoNota tipo, String motivo, List<VentaRequest.Item> items) {
        return new NotaRequest(10L, tipo, motivo, "Sustento de prueba", null, items);
    }

    @Test
    void anulacionTotalCopiaLasLineasYUsaLaSerieFC01() {
        NotaResponse nota = notaService.emitir(pedido(TipoNota.CREDITO, "01", null), "contabilidad1");

        assertEquals("FC01-1", nota.numero());
        assertEquals(new BigDecimal("3682.19"), nota.montoTotal());
        assertEquals(2, nota.items().size());
        assertEquals("Anulación de la operación", nota.motivo());
        verify(facturacionService).emitir(any(Nota.class));
    }

    @Test
    void devolucionParcialPorItem() {
        NotaResponse nota = notaService.emitir(pedido(TipoNota.CREDITO, "07", List.of(
                new VentaRequest.Item(null, "LAP-01", "Laptop devuelta", "NIU", BigDecimal.ONE, new BigDecimal("1500.00")))),
                "contabilidad1");

        assertEquals(new BigDecimal("1500.00"), nota.montoSinIgv());
        assertEquals(new BigDecimal("1770.00"), nota.montoTotal());
    }

    @Test
    void unaNotaDeCreditoNoPuedeSuperarElSaldo() {
        when(notaRepository.totalPorVentaYTipo(10L, TipoNota.CREDITO)).thenReturn(new BigDecimal("3000.00"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> notaService.emitir(
                pedido(TipoNota.CREDITO, "07", List.of(new VentaRequest.Item(null, null, "Laptop", "NIU",
                        BigDecimal.ONE, new BigDecimal("1500.00")))), "contabilidad1"));
        assertTrue(error.getMessage().contains("supera el saldo"), error.getMessage());
        verify(notaRepository, never()).save(any());
    }

    @Test
    void laAnulacionTotalNoProcedeSiYaHayNotas() {
        when(notaRepository.totalPorVentaYTipo(10L, TipoNota.CREDITO)).thenReturn(new BigDecimal("100.00"));

        assertThrows(ReglaNegocioException.class,
                () -> notaService.emitir(pedido(TipoNota.CREDITO, "06", null), "contabilidad1"));
    }

    @Test
    void notaDeDebitoPorInteresesUsaLaSerieFD01() {
        NotaResponse nota = notaService.emitir(pedido(TipoNota.DEBITO, "01", List.of(
                new VentaRequest.Item(null, null, "Intereses por mora", "ZZ", BigDecimal.ONE, new BigDecimal("50.00")))),
                "contabilidad1");

        assertEquals("FD01-1", nota.numero());
        assertEquals(new BigDecimal("59.00"), nota.montoTotal());
    }

    @Test
    void soloSobreComprobantesAceptadosPorSunat() {
        venta.getSunat().setEstado(EstadoSunat.PENDIENTE);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> notaService.emitir(pedido(TipoNota.CREDITO, "01", null), "contabilidad1"));
        assertTrue(error.getMessage().contains("aceptado por SUNAT"));
    }

    @Test
    void motivoQueNoExisteEnElCatalogo() {
        assertThrows(IllegalArgumentException.class,
                () -> notaService.emitir(pedido(TipoNota.DEBITO, "09", null), "contabilidad1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void elXmlDeLaNotaReferenciaALaFacturaConElMotivo() {
        notaService.emitir(pedido(TipoNota.CREDITO, "01", null), "contabilidad1");
        ArgumentCaptor<Nota> captor = ArgumentCaptor.forClass(Nota.class);
        verify(facturacionService).emitir(captor.capture());
        Nota nota = captor.getValue();

        FacturacionService real = new FacturacionService(new SimuladorSunat(), new FacturacionProperties("simulador",
                new FacturacionProperties.Apisunat(null, null, null),
                new FacturacionProperties.Emisor("20000000001", "PRINCIPE S.A.C.", null, null, null)));
        DocumentoElectronico doc = real.documentoDe(nota);
        assertEquals("07", doc.tipo());
        assertEquals("FC01", doc.serie());
        assertEquals("F001-00000025", doc.referencia().numero());
        assertTrue(SimuladorSunat.validar(doc).isEmpty(), "el simulador debe aceptarla");

        Map<String, Object> ubl = DocumentoUbl.construir(doc, new FacturacionProperties.Emisor("20000000001",
                "PRINCIPE S.A.C.", null, null, null));
        Map<String, Object> discrepancia = (Map<String, Object>) ubl.get("cac:DiscrepancyResponse");
        assertEquals(Map.of("_text", "01"), discrepancia.get("cbc:ResponseCode"));
        assertTrue(ubl.containsKey("cac:CreditNoteLine"));
        assertTrue(!ubl.containsKey("cbc:InvoiceTypeCode"));
        Map<String, Object> linea = ((List<Map<String, Object>>) ubl.get("cac:CreditNoteLine")).get(0);
        assertTrue(linea.containsKey("cbc:CreditedQuantity"));
    }
}
