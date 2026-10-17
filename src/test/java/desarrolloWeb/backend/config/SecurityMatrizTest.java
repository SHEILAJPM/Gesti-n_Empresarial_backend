package desarrolloWeb.backend.config;

import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.ProveedorRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Matriz de seguridad")
class SecurityMatrizTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private VentaRepository ventaRepository;

    private Long ventaId;

    @BeforeEach
    void setUp() {
        Cliente cliente = clienteRepository.save(new Cliente("10203040", "Carlos Mendoza", TipoDocumento.DNI));
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setTipoComprobante(TipoComprobante.BOLETA);
        venta.setNumeroComprobante("B001-900");
        venta.setFecha(LocalDate.of(2026, 8, 15));
        venta.setMontoSinIgv(new BigDecimal("300.00"));
        venta.setIgv(new BigDecimal("54.00"));
        venta.setMontoTotal(new BigDecimal("354.00"));
        venta.setTasaAplicada(new BigDecimal("0.1800"));
        venta.setEstado(EstadoVenta.EMITIDA);
        ventaId = ventaRepository.save(venta).getId();
    }

    @Test
    @DisplayName("01 · sin credenciales · GET /api/compras → 401 JSON")
    void caso01() throws Exception {
        mockMvc.perform(get("/api/compras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("02 · admin con clave incorrecta · GET /api/compras → 401 JSON")
    void caso02() throws Exception {
        mockMvc.perform(get("/api/compras").with(httpBasic("admin", "ClaveMala")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("03 · rrhh1 · GET /api/compras → 403 JSON")
    void caso03() throws Exception {
        mockMvc.perform(get("/api/compras").with(httpBasic("rrhh1", "Rrhh123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("04 · contabilidad1 · GET /api/compras → 200")
    void caso04() throws Exception {
        mockMvc.perform(get("/api/compras").with(httpBasic("contabilidad1", "Conta123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("05 · contabilidad1 · POST /api/compras → 201 (contabilidad registra facturas de compra)")
    void caso05() throws Exception {
        proveedorRepository.save(new Proveedor("20123456789", "Distribuidora ACME S.A.C.", null));
        mockMvc.perform(post("/api/compras").with(httpBasic("contabilidad1", "Conta123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruc\":\"20123456789\",\"numeroComprobante\":\"F001-1\","
                                + "\"montoSinIgv\":100.00,\"fecha\":\"2026-08-10\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("06 · administracion1 · POST /api/proveedores → 201")
    void caso06() throws Exception {
        mockMvc.perform(post("/api/proveedores").with(httpBasic("administracion1", "AdminArea123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruc\":\"20111222333\",\"razonSocial\":\"Proveedor Matriz S.A.C.\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("07 · administracion1 · GET /api/ventas → 200")
    void caso07() throws Exception {
        mockMvc.perform(get("/api/ventas").with(httpBasic("administracion1", "AdminArea123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("08 · administracion1 · PATCH /api/ventas/{id}/anular → 403")
    void caso08() throws Exception {
        mockMvc.perform(patch("/api/ventas/" + ventaId + "/anular").with(httpBasic("administracion1", "AdminArea123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("09 · contabilidad1 · PATCH /api/ventas/{id}/anular → 200")
    void caso09() throws Exception {
        mockMvc.perform(patch("/api/ventas/" + ventaId + "/anular").with(httpBasic("contabilidad1", "Conta123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("10 · administracion1 · GET /api/tributario/resumen → 403")
    void caso10() throws Exception {
        mockMvc.perform(get("/api/tributario/resumen?desde=2026-08-01&hasta=2026-08-31")
                        .with(httpBasic("administracion1", "AdminArea123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11 · contabilidad1 · PUT /api/tributario/configuracion/IGV → 403")
    void caso11() throws Exception {
        mockMvc.perform(put("/api/tributario/configuracion/IGV").with(httpBasic("contabilidad1", "Conta123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tasa\":0.1800}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12 · admin · PUT /api/tributario/configuracion/IGV → 200")
    void caso12() throws Exception {
        mockMvc.perform(put("/api/tributario/configuracion/IGV").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tasa\":0.1800}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("IGV"));
    }

    @Test
    @DisplayName("Regresión · rrhh1 · GET /api/personal → 200 (personal sigue funcionando)")
    void regresionPersonal() throws Exception {
        mockMvc.perform(get("/api/personal").with(httpBasic("rrhh1", "Rrhh123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login · contabilidad1 · GET /api/personal → 200 y POST → 403 (solo consulta para pagos)")
    void contabilidadConsultaPersonal() throws Exception {
        mockMvc.perform(get("/api/personal").with(httpBasic("contabilidad1", "Conta123")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/personal").with(httpBasic("contabilidad1", "Conta123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login · sin credenciales desde el frontend → 401 sin WWW-Authenticate")
    void frontendSinVentanaNativa() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @DisplayName("Regresión · contabilidad1 · GET /api/tributario/resumen → 200 (sin compras, por pagar = débito 54.00)")
    void regresionResumen() throws Exception {
        mockMvc.perform(get("/api/tributario/resumen?desde=2026-08-01&hasta=2026-08-31")
                        .with(httpBasic("contabilidad1", "Conta123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.igvVentas").value(54.00))
                .andExpect(jsonPath("$.igvPorPagar").value(54.00));
    }

    @Test
    @DisplayName("Planilla · contabilidad1 · POST /api/planillas/generar → 403 (la genera RRHH)")
    void planillaContabilidadNoGenera() throws Exception {
        mockMvc.perform(post("/api/planillas/generar").with(httpBasic("contabilidad1", "Conta123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"anio\":2026,\"mes\":9}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Planilla · rrhh1 · PATCH /api/planillas/1/aprobar → 403 (la aprueba Contabilidad)")
    void planillaRrhhNoAprueba() throws Exception {
        mockMvc.perform(patch("/api/planillas/1/aprobar").with(httpBasic("rrhh1", "Rrhh123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fechaPago\":\"2026-09-30\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Planilla · contabilidad1 · GET /api/planillas → 200")
    void planillaContabilidadConsulta() throws Exception {
        mockMvc.perform(get("/api/planillas").with(httpBasic("contabilidad1", "Conta123")))
                .andExpect(status().isOk());
    }
}
