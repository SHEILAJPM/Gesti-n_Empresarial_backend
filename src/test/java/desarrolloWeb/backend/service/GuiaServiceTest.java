package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoGuia;
import desarrolloWeb.backend.dto.GuiaRequest;
import desarrolloWeb.backend.dto.GuiaResponse;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Guia;
import desarrolloWeb.backend.entity.ModalidadTraslado;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.repository.GuiaRepository;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Guías de remisión")
class GuiaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 7);

    @Mock private GuiaRepository guiaRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private FacturacionService facturacionService;

    private GuiaService guiaService;
    private Venta venta;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(ZonedDateTime.of(HOY.atTime(10, 0), PortalService.ZONA).toInstant(), PortalService.ZONA);
        guiaService = new GuiaService(guiaRepository, ventaRepository, facturacionService, reloj);

        Cliente cliente = new Cliente("20512345671", "Distribuidora El Sol S.A.C.", TipoDocumento.RUC);
        venta = new Venta();
        venta.setId(5L);
        venta.setCliente(cliente);
        venta.setTipoComprobante(TipoComprobante.FACTURA);
        venta.setNumeroComprobante("F001-12");
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.agregarItem(item("LAP-001", "Laptop", "NIU", "3"));
        venta.agregarItem(item("SRV-INST", "Instalación", "ZZ", "3"));

        lenient().when(ventaRepository.findById(5L)).thenReturn(Optional.of(venta));
        lenient().when(guiaRepository.maximoCorrelativo(anyString(), anyInt())).thenReturn(null);
        lenient().when(facturacionService.siguienteCorrelativo(anyString(), anyString(), anyInt())).thenAnswer(i -> i.getArgument(2));
        lenient().when(guiaRepository.save(any(Guia.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static VentaItem item(String codigo, String desc, String unidad, String cant) {
        VentaItem i = new VentaItem();
        i.setCodigo(codigo);
        i.setDescripcion(desc);
        i.setUnidad(unidad);
        i.setCantidad(new BigDecimal(cant));
        return i;
    }

    private static GuiaRequest privada(Long ventaId, List<GuiaRequest.Item> items, String placa) {
        return new GuiaRequest(ventaId, "01", null, null, null, new BigDecimal("6.5"), 3, ModalidadTraslado.PRIVADO,
                null, null, placa, "41234567", "Carlos Ruiz Pérez", "Q41234567",
                "150122", "Av. José Larco 1150, Miraflores", "150130", "Av. Javier Prado 4200, Surco", null, items);
    }

    @Test
    void desdeUnaVentaTomaElClienteYSoloLosBienes() {
        GuiaResponse guia = guiaService.emitir(privada(5L, null, "ABC-123"), "almacen1");

        assertEquals("T001-1", guia.numero());
        assertEquals("20512345671", guia.destinatarioDocumento());
        assertEquals("6", guia.destinatarioTipoDocumento());
        assertEquals(1, guia.items().size(), "la instalación (servicio) no se traslada");
        assertEquals("F001-12", guia.comprobanteRelacionado());
        assertEquals("Venta", guia.motivo());
        verify(facturacionService).emitir(any(Guia.class));
    }

    @Test
    void vehiculoPropioExigePlacaYConductor() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> guiaService.emitir(privada(5L, null, null), "almacen1"));
        assertTrue(e.getMessage().contains("placa"));
        verify(guiaRepository, never()).save(any());
    }

    @Test
    void transporteContratadoExigeRucYRazonSocial() {
        GuiaRequest publica = new GuiaRequest(null, "04", null, "20512345671", "Distribuidora El Sol S.A.C.",
                new BigDecimal("12"), 2, ModalidadTraslado.PUBLICO, null, null, null, null, null, null,
                "150122", "Almacén Miraflores", "150101", "Tienda Cercado",
                null, List.of(new GuiaRequest.Item(null, "Cajas de papel", "BX", new BigDecimal("10"))));

        assertThrows(IllegalArgumentException.class, () -> guiaService.emitir(publica, "almacen1"));
    }

    @Test
    void elTrasladoNoEmpiezaAntesDeLaEmision() {
        GuiaRequest ayer = new GuiaRequest(5L, "01", HOY.minusDays(1), null, null, BigDecimal.ONE, null,
                ModalidadTraslado.PRIVADO, null, null, "ABC123", "41234567", "Carlos", "Q1", "150122", "A", "150130", "B",
                null, null);
        assertThrows(IllegalArgumentException.class, () -> guiaService.emitir(ayer, "almacen1"));
    }

    @Test
    void sinVentaNiBienesNoHayGuia() {
        GuiaRequest vacia = new GuiaRequest(null, "13", null, "41223344", "Roberto Vílchez", BigDecimal.ONE, null,
                ModalidadTraslado.PRIVADO, null, null, "ABC123", "41234567", "Carlos", "Q1", "150122", "A", "150130", "B",
                null, List.of());
        assertThrows(IllegalArgumentException.class, () -> guiaService.emitir(vacia, "almacen1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void elXmlLlevaMotivoTrasladoConductorYDocumentoRelacionado() {
        guiaService.emitir(privada(5L, null, "abc-123"), "almacen1");
        ArgumentCaptor<Guia> captor = ArgumentCaptor.forClass(Guia.class);
        verify(facturacionService).emitir(captor.capture());

        FacturacionProperties.Emisor emisor = new FacturacionProperties.Emisor("20000000001", "PRINCIPE S.A.C.", null, null, "150101");
        FacturacionService real = new FacturacionService(new SimuladorSunat(),
                new FacturacionProperties("simulador", new FacturacionProperties.Apisunat(null, null, null), emisor));
        DocumentoGuia doc = real.documentoDe(captor.getValue());
        assertEquals("20000000001-09-T001-00000001", doc.fileName("20000000001"));
        assertEquals("F001-00000012", doc.relacionado().numero());
        // El simulador valida contra hoy: se comprueba con la fecha real
        DocumentoGuia hoyReal = new DocumentoGuia(doc.serie(), doc.correlativo(), LocalDate.now(), LocalDate.now(),
                doc.motivoCodigo(), doc.motivoDescripcion(), doc.destinatarioTipoDocumento(), doc.destinatarioDocumento(),
                doc.destinatarioNombre(), doc.pesoBrutoKg(), doc.numeroBultos(), doc.modalidad(), null, null,
                doc.vehiculoPlaca(), doc.conductorDni(), doc.conductorNombres(), doc.conductorLicencia(),
                doc.partidaUbigeo(), doc.partidaDireccion(), doc.llegadaUbigeo(), doc.llegadaDireccion(), null,
                doc.relacionado(), doc.lineas());
        assertTrue(SimuladorSunat.validarGuia(hoyReal).isEmpty(), SimuladorSunat.validarGuia(hoyReal).toString());

        Map<String, Object> ubl = DocumentoUbl.construirGuia(doc, emisor);
        Map<String, Object> envio = (Map<String, Object>) ubl.get("cac:Shipment");
        assertEquals("01", ((Map<String, Object>) envio.get("cbc:HandlingCode")).get("_text"));
        Map<String, Object> etapa = (Map<String, Object>) envio.get("cac:ShipmentStage");
        assertEquals(Map.of("_text", "02"), etapa.get("cbc:TransportModeCode"));
        assertTrue(etapa.containsKey("cac:DriverPerson"));
        assertFalse(etapa.containsKey("cac:CarrierParty"));
        Map<String, Object> equipo = (Map<String, Object>) ((Map<String, Object>) envio.get("cac:TransportHandlingUnit")).get("cac:TransportEquipment");
        assertEquals(Map.of("_text", "ABC123"), equipo.get("cbc:ID"));
        assertEquals(1, ((List<?>) ubl.get("cac:DespatchLine")).size());
        assertTrue(ubl.containsKey("cac:AdditionalDocumentReference"));
    }
}
