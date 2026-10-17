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
}
