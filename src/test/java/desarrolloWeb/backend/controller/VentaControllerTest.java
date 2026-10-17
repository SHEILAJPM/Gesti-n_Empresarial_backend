package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.config.SecurityConfig;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.security.RestAccessDeniedHandler;
import desarrolloWeb.backend.security.RestAuthenticationEntryPoint;
import desarrolloWeb.backend.service.VentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VentaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@DisplayName("VentaController")
class VentaControllerTest {

    private static final String FECHA = LocalDate.now().minusDays(1).toString();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService ventaService;

    @Test
    @WithMockUser(username = "administracion1", roles = "ADMINISTRACION")
    @DisplayName("POST válido responde 201 con header Location y el cuerpo de la venta")
    void registrarResponde201() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class))).thenReturn(ventaResponse());

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("10203040", "B001-1", "300.00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/ventas/7"))
                .andExpect(jsonPath("$.tipoComprobante").value("BOLETA"))
                .andExpect(jsonPath("$.igv").value(54.00));
    }

    @Test
    @WithMockUser(username = "administracion1", roles = "ADMINISTRACION")
    @DisplayName("POST con monto negativo y comprobante mal formado responde 400 con errores por campo")
    void registrarResponde400PorValidacion() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("10203040", "X-1", "-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.montoSinIgv").value("El monto sin IGV debe ser mayor a cero"))
                .andExpect(jsonPath("$.errores.numeroComprobante")
                        .value("El comprobante debe tener el formato F001-123 o B001-123"));

        verify(ventaService, never()).registrar(any());
    }

    @Test
    @WithMockUser(username = "contabilidad1", roles = "CONTABILIDAD")
    @DisplayName("GET de una venta inexistente responde 404 con ErrorResponse")
    void buscarResponde404() throws Exception {
        when(ventaService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe una venta con id 99"));

        mockMvc.perform(get("/api/ventas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("No existe una venta con id 99"))
                .andExpect(jsonPath("$.path").value("/api/ventas/99"));
    }

    @Test
    @WithMockUser(username = "administracion1", roles = "ADMINISTRACION")
    @DisplayName("POST con comprobante duplicado responde 409")
    void registrarResponde409() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class)))
                .thenThrow(new ReglaNegocioException("Ya existe una venta con el comprobante B001-1"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("10203040", "B001-1", "300.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya existe una venta con el comprobante B001-1"));
    }

    @Test
    @WithMockUser(username = "administracion1", roles = "ADMINISTRACION")
    @DisplayName("POST con Content-Type text/plain responde 415")
    void registrarResponde415() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("sin credenciales responde 401 en JSON")
    void sinCredencialesResponde401() throws Exception {
        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "administracion1", roles = "ADMINISTRACION")
    @DisplayName("ADMINISTRACION no puede anular: 403 en JSON")
    void administracionNoAnula() throws Exception {
        mockMvc.perform(patch("/api/ventas/7/anular"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(ventaService, never()).anular(any());
    }

    private String json(String documento, String numero, String monto) {
        return """
                {"documentoCliente":"%s","numeroComprobante":"%s","montoSinIgv":%s,"fecha":"%s"}
                """.formatted(documento, numero, monto, FECHA);
    }

    private VentaResponse ventaResponse() {
        return new VentaResponse(7L, 1L, "10203040", "Carlos Mendoza", TipoComprobante.BOLETA, "B001-1",
                LocalDate.now().minusDays(1), new BigDecimal("300.00"), new BigDecimal("54.00"),
                new BigDecimal("354.00"), new BigDecimal("0.1800"), EstadoVenta.EMITIDA);
    }
}
