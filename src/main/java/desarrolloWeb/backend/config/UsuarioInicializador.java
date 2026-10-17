package desarrolloWeb.backend.config;

import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.service.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/* Crea los usuarios base la primera vez que arranca el sistema */
@Component
public class UsuarioInicializador implements ApplicationRunner {

    private final UsuarioService usuarioService;

    public UsuarioInicializador(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        usuarioService.crearSiNoExiste("admin", "Admin123", Rol.ADMIN);
        usuarioService.crearSiNoExiste("rrhh1", "Rrhh123", Rol.RRHH);
        usuarioService.crearSiNoExiste("contabilidad1", "Conta123", Rol.CONTABILIDAD);
        usuarioService.crearSiNoExiste("administracion1", "AdminArea123", Rol.ADMINISTRACION);
    }
}
