package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.RespuestaFacturacion;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.util.DocumentoUbl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Facturación electrónica: simulador, UBL y cliente APISUNAT")
class FacturacionElectronicaTest {

    private static final FacturacionProperties PROPIEDADES = new FacturacionProperties("apisunat",
            new FacturacionProperties.Apisunat("https://back.apisunat.com", "persona-1", "token-dev"),
            new FacturacionProperties.Emisor("20000000001", "PRINCIPE S.A.C.", "", "", "150101"));

    private static DocumentoElectronico factura(LocalDate fecha, String docCliente) {
        return new DocumentoElectronico("01", "F001", 12, fecha, "6", docCliente, "Comercial Lima S.A.C.",
                "Venta de bienes y/o servicios", new BigDecimal("100.00"), new BigDecimal("18.00"),
                new BigDecimal("118.00"), new BigDecimal("0.1800"));
    }

    @Test
    void numeroYNombreDeArchivoConElFormatoDeSunat() {
        DocumentoElectronico d = factura(LocalDate.now(), "20555666777");
        assertEquals("F001-00000012", d.numero());
        assertEquals("20000000001-01-F001-00000012", d.fileName("20000000001"));
    }

    @Test
    void simuladorAceptaUnaFacturaValida() {
        SimuladorSunat simulador = new SimuladorSunat();
        RespuestaFacturacion envio = simulador.enviar(factura(LocalDate.now(), "20555666777"));

        assertEquals(EstadoSunat.PENDIENTE, envio.estado());
        assertEquals(EstadoSunat.ACEPTADO, simulador.consultar(envio.documentId()).estado());
    }

    @Test
    void simuladorRechazaFacturaAClienteConDniYFechaFueraDePlazo() {
        SimuladorSunat simulador = new SimuladorSunat();

        RespuestaFacturacion conDni = simulador.consultar(simulador.enviar(factura(LocalDate.now(), "10203040")).documentId());
        RespuestaFacturacion antigua = simulador.consultar(simulador.enviar(factura(LocalDate.now().minusDays(10), "20555666777")).documentId());

        assertEquals(EstadoSunat.RECHAZADO, conDni.estado());
        assertTrue(conDni.mensaje().contains("RUC"));
        assertEquals(EstadoSunat.RECHAZADO, antigua.estado());
        assertTrue(antigua.mensaje().contains("plazo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentoUblLlevaEmisorClienteImpuestosYTotales() {
        Map<String, Object> ubl = DocumentoUbl.construir(factura(LocalDate.of(2026, 10, 17), "20555666777"), PROPIEDADES.emisor());

        assertEquals(Map.of("_text", "F001-00000012"), ubl.get("cbc:ID"));
        assertEquals("01", ((Map<String, Object>) ubl.get("cbc:InvoiceTypeCode")).get("_text"));
        Map<String, Object> totales = (Map<String, Object>) ubl.get("cac:LegalMonetaryTotal");
        assertEquals("118.00", ((Map<String, Object>) totales.get("cbc:PayableAmount")).get("_text"));
        Map<String, Object> impuesto = (Map<String, Object>) ((Map<String, Object>) ubl.get("cac:TaxTotal")).get("cbc:TaxAmount");
        assertEquals("18.00", impuesto.get("_text"));
        assertEquals("SON CIENTO DIECIOCHO CON 00/100 SOLES",
                ((Map<String, Object>) ((List<Object>) ubl.get("cbc:Note")).get(0)).get("_text"));
        assertTrue(ubl.containsKey("cac:PaymentTerms"), "la factura indica la forma de pago");
    }

    @Test
    void apisunatEnviaPorSendBillYConsultaElEstado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ApisunatFacturacion apisunat = new ApisunatFacturacion(PROPIEDADES, builder);

        servidor.expect(requestTo("https://back.apisunat.com/personas/v1/sendBill"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.personaId").value("persona-1"))
                .andExpect(jsonPath("$.personaToken").value("token-dev"))
                .andExpect(jsonPath("$.fileName").value("20000000001-01-F001-00000012"))
                .andExpect(jsonPath("$.documentBody['cbc:ID']._text").value("F001-00000012"))
                .andRespond(withSuccess("{\"status\":\"PENDIENTE\",\"documentId\":\"doc-123\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://back.apisunat.com/documents/doc-123/getById"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"ACEPTADO\",\"xml\":\"https://x/xml\",\"cdr\":\"https://x/cdr\","
                        + "\"faults\":[],\"notes\":[]}", MediaType.APPLICATION_JSON));

        RespuestaFacturacion envio = apisunat.enviar(factura(LocalDate.now(), "20555666777"));
        RespuestaFacturacion estado = apisunat.consultar("doc-123");

        assertEquals(EstadoSunat.PENDIENTE, envio.estado());
        assertEquals("doc-123", envio.documentId());
        assertEquals(EstadoSunat.ACEPTADO, estado.estado());
        assertEquals("https://x/cdr", estado.cdrUrl());
        assertEquals("https://back.apisunat.com/documents/doc-123/getPDF/A4/20000000001-01-F001-00000012.pdf",
                apisunat.urlPdf("doc-123", "20000000001-01-F001-00000012"));
        servidor.verify();
    }

    @Test
    void apisunatDevuelveErrorLegibleSiRechazaElEnvio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ApisunatFacturacion apisunat = new ApisunatFacturacion(PROPIEDADES, builder);
        servidor.expect(requestTo("https://back.apisunat.com/personas/v1/sendBill"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"ERROR\",\"error\":{\"message\":\"Documento duplicado\"}}"));

        RespuestaFacturacion r = apisunat.enviar(factura(LocalDate.now(), "20555666777"));

        assertEquals(EstadoSunat.ERROR, r.estado());
        assertTrue(r.mensaje().contains("Documento duplicado"));
    }

    @Test
    void apisunatUsaElCorrelativoMayorEntreElPortalYElSistema() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        ApisunatFacturacion apisunat = new ApisunatFacturacion(PROPIEDADES, builder);
        servidor.expect(requestTo("https://back.apisunat.com/personas/lastDocument"))
                .andExpect(jsonPath("$.serie").value("F001"))
                .andRespond(withSuccess("{\"lastNumber\":\"00000007\",\"suggestedNumber\":\"00000008\"}", MediaType.APPLICATION_JSON));

        assertEquals(8, apisunat.siguienteCorrelativo("01", "F001", 3));
    }
}
