package desarrolloWeb.backend.config;

import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Usuarios iniciales: las contraseñas vienen de la configuración")
class UsuarioInicializadorTest {

    @Test
    void creaSoloLosUsuariosConContrasenaConfigurada() {
        UsuarioService usuarios = mock(UsuarioService.class);

        new UsuarioInicializador(usuarios, "ClaveAdminSegura", "", null, "  ").run(null);

        verify(usuarios).crearSiNoExiste("admin", "ClaveAdminSegura", Rol.ADMIN);
        verify(usuarios, never()).crearSiNoExiste(eq("rrhh1"), anyString(), eq(Rol.RRHH));
        verify(usuarios, never()).crearSiNoExiste(eq("contabilidad1"), anyString(), eq(Rol.CONTABILIDAD));
        verify(usuarios, never()).crearSiNoExiste(eq("administracion1"), anyString(), eq(Rol.ADMINISTRACION));
    }
}
