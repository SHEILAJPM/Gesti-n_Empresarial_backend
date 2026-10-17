package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.DocumentoConsultado;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ServicioExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Consulta de DNI y RUC (Decolecta)")
class ConsultaDocumentoServiceTest {

    private static final String URL = "https://api.decolecta.com";

    private MockRestServiceServer servidor;
    private ConsultaDocumentoService servicio;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        servicio = new ConsultaDocumentoService(URL, "token-prueba", builder);
    }

    @Test
    void dniDevuelveNombresYApellidosCapitalizados() {
        servidor.expect(requestTo(URL + "/v1/reniec/dni?numero=46027897"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-prueba"))
                .andRespond(withSuccess("""
                        {"first_name":"ROXANA KARINA","first_last_name":"DELGADO","second_last_name":"HUAMANI",
                         "full_name":"DELGADO HUAMANI ROXANA KARINA","document_number":"46027897"}""",
                        MediaType.APPLICATION_JSON));

        DocumentoConsultado d = servicio.consultar("46027897");

        assertEquals(TipoDocumento.DNI, d.tipo());
        assertEquals("Roxana Karina Delgado Huamani", d.nombre());
        assertNull(d.direccion());
        servidor.verify();
    }

    @Test
    void rucDevuelveRazonSocialDireccionYEstado() {
        servidor.expect(requestTo(URL + "/v1/sunat/ruc?numero=20601030013"))
                .andRespond(withSuccess("""
                        {"razon_social":"REXTIE S.A.C.","numero_documento":"20601030013","estado":"ACTIVO",
                         "condicion":"HABIDO","direccion":"AV. JOSE GALVEZ BARRENECHEA NRO 566 INT. 101 URB. CORPAC ",
                         "distrito":"SAN ISIDRO","provincia":"LIMA","departamento":"LIMA"}""",
                        MediaType.APPLICATION_JSON));

        DocumentoConsultado d = servicio.consultar("20601030013");

        assertEquals(TipoDocumento.RUC, d.tipo());
        assertEquals("REXTIE S.A.C.", d.nombre());
        assertEquals("AV. JOSE GALVEZ BARRENECHEA NRO 566 INT. 101 URB. CORPAC, SAN ISIDRO, LIMA, LIMA", d.direccion());
        assertEquals("ACTIVO", d.estado());
        assertEquals("HABIDO", d.condicion());
    }

    @Test
    void laSegundaConsultaDelMismoDocumentoNoGastaCuota() {
        servidor.expect(requestTo(URL + "/v1/reniec/dni?numero=46027897"))
                .andRespond(withSuccess("{\"first_name\":\"ANA\",\"first_last_name\":\"RUIZ\"}", MediaType.APPLICATION_JSON));

        servicio.consultar("46027897");
        DocumentoConsultado otraVez = servicio.consultar("46027897");

        assertEquals("Ana Ruiz", otraVez.nombre());
        servidor.verify();
    }

    @Test
    void documentoInexistenteEs404() {
        servidor.expect(requestTo(URL + "/v1/sunat/ruc?numero=20999999999"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).body("{\"message\":\"ruc no valido\"}"));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.consultar("20999999999"));
    }

    @Test
    void tokenInvalidoOLimiteAlcanzadoSon503() {
        servidor.expect(requestTo(URL + "/v1/reniec/dni?numero=11111111")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        servidor.expect(requestTo(URL + "/v1/reniec/dni?numero=22222222")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(ServicioExternoException.class, () -> servicio.consultar("11111111"));
        assertThrows(ServicioExternoException.class, () -> servicio.consultar("22222222"));
    }

    @Test
    void documentoMalFormadoNoLlegaAConsultarse() {
        assertThrows(DocumentoInvalidoException.class, () -> servicio.consultar("123"));
        assertThrows(DocumentoInvalidoException.class, () -> servicio.consultar("99123456789"));
        servidor.verify();
    }

    @Test
    void sinTokenAvisaQueFaltaConfigurar() {
        ConsultaDocumentoService sinToken = new ConsultaDocumentoService(URL, "", RestClient.builder());

        ServicioExternoException e = assertThrows(ServicioExternoException.class, () -> sinToken.consultar("46027897"));
        assertEquals(true, e.getMessage().contains("DECOLECTA_TOKEN"));
    }
}
