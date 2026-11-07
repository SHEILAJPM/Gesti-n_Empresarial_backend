package desarrolloWeb.backend.service;

import desarrolloWeb.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Login y usuarios en base de datos")
class UsuarioLoginTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Los 4 usuarios iniciales ingresan con su rol")
    void usuariosIniciales() throws Exception {
        String[][] usuarios = {
                {"admin", "Admin123", "ADMIN"},
                {"rrhh1", "Rrhh123", "RRHH"},
                {"contabilidad1", "Conta123", "CONTABILIDAD"},
                {"administracion1", "AdminArea123", "ADMINISTRACION"}
        };

        for (String[] u : usuarios) {
            mockMvc.perform(get("/api/auth/me").with(httpBasic(u[0], u[1])))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario").value(u[0]))
                    .andExpect(jsonPath("$.rol").value(u[2]));
        }
    }

    @Test
    @DisplayName("admin crea un usuario nuevo y ese usuario puede ingresar")
    void adminCreaUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rrhh2\",\"password\":\"Clave123\",\"rol\":\"RRHH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("rrhh2"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/auth/me").with(httpBasic("rrhh2", "Clave123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("RRHH"));

        mockMvc.perform(get("/api/personal").with(httpBasic("rrhh2", "Clave123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Usuario duplicado → 409/400 y no se crea")
    void usuarioDuplicado() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rrhh1\",\"password\":\"Clave123\",\"rol\":\"RRHH\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Solo admin gestiona usuarios")
    void soloAdmin() throws Exception {
        mockMvc.perform(get("/api/usuarios").with(httpBasic("rrhh1", "Rrhh123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").with(httpBasic("admin", "Admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un usuario desactivado ya no puede ingresar")
    void usuarioDesactivado() throws Exception {
        Long id = usuarioRepository.findByUsername("administracion1").orElseThrow().getId();

        mockMvc.perform(patch("/api/usuarios/" + id + "/estado?activo=false").with(httpBasic("admin", "Admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/auth/me").with(httpBasic("administracion1", "AdminArea123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin no puede desactivarse a sí mismo")
    void adminNoSeDesactiva() throws Exception {
        Long id = usuarioRepository.findByUsername("admin").orElseThrow().getId();

        mockMvc.perform(patch("/api/usuarios/" + id + "/estado?activo=false").with(httpBasic("admin", "Admin123")))
                .andExpect(status().is4xxClientError());
    }

    private String token(String usuario, String clave) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.token");
    }

    @Test
    @DisplayName("Login devuelve un JWT que sirve para las demás peticiones")
    void loginConJwt() throws Exception {
        String jwt = token("rrhh1", "Rrhh123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("rrhh1"))
                .andExpect(jsonPath("$.rol").value("RRHH"));
        mockMvc.perform(get("/api/personal").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta → 401 con mensaje claro")
    void loginIncorrecto() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"rrhh1\",\"clave\":\"Mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    @DisplayName("Un token alterado o inventado → 401")
    void tokenInvalido() throws Exception {
        String jwt = token("rrhh1", "Rrhh123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Si desactivan la cuenta, su token deja de servir aunque no haya vencido")
    void tokenDeCuentaDesactivada() throws Exception {
        String jwt = token("administracion1", "AdminArea123");
        Long id = usuarioRepository.findByUsername("administracion1").orElseThrow().getId();
        mockMvc.perform(patch("/api/usuarios/" + id + "/estado?activo=false").with(httpBasic("admin", "Admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cambiar la contraseña: exige la actual, aplica la política y cierra las demás sesiones")
    void cambiarContrasena() throws Exception {
        String viejo = token("contabilidad1", "Conta123");

        mockMvc.perform(put("/api/auth/clave").header("Authorization", "Bearer " + viejo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"Mala\",\"claveNueva\":\"Nueva2026x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta"));
        mockMvc.perform(put("/api/auth/clave").header("Authorization", "Bearer " + viejo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"Conta123\",\"claveNueva\":\"solotexto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña debe combinar letras y números"));

        // El JWT guarda la hora de emisión al segundo: se espera un segundo para distinguir el token viejo del nuevo
        Thread.sleep(1100);
        String cuerpo = mockMvc.perform(put("/api/auth/clave").header("Authorization", "Bearer " + viejo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveActual\":\"Conta123\",\"claveNueva\":\"Nueva2026x\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nuevo = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + viejo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + nuevo))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").with(httpBasic("contabilidad1", "Nueva2026x")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El administrador restablece la contraseña de otra cuenta; los demás no pueden")
    void adminRestableceContrasena() throws Exception {
        Long id = usuarioRepository.findByUsername("rrhh1").orElseThrow().getId();

        mockMvc.perform(put("/api/usuarios/" + id + "/clave").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"claveNueva\":\"Rrhh2026seg\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").with(httpBasic("rrhh1", "Rrhh2026seg")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/usuarios/" + id + "/clave").with(httpBasic("rrhh1", "Rrhh2026seg"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"claveNueva\":\"Otra2026seg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cinco contraseñas incorrectas seguidas bloquean el login (429)")
    void bloqueoPorIntentos() throws Exception {
        // Usuario propio: el límite vive en memoria y no debe afectar a las otras pruebas
        mockMvc.perform(post("/api/usuarios").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bloqueo1\",\"password\":\"Clave2026\",\"rol\":\"RRHH\"}"))
                .andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"usuario\":\"bloqueo1\",\"clave\":\"Mala" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // Mientras dura el bloqueo ni la contraseña correcta entra
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"bloqueo1\",\"clave\":\"Clave2026\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("Una contraseña débil no se acepta al crear usuarios")
    void claveDebilAlCrear() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(httpBasic("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"debil1\",\"password\":\"123456\",\"rol\":\"RRHH\"}"))
                .andExpect(status().isBadRequest());
    }
}
