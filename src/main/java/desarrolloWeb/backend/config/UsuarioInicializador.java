package desarrolloWeb.backend.config;

import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/* Crea los usuarios base la primera vez que arranca el sistema.
   Las contraseñas salen de backend/.env (ADMIN_PASSWORD, RRHH_PASSWORD...), nunca del código:
   si una no está configurada, ese usuario no se crea. El perfil local y los tests traen las de desarrollo. */
@Component
public class UsuarioInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicializador.class);

    private final UsuarioService usuarioService;
    private final String claveAdmin;
    private final String claveRrhh;
    private final String claveContabilidad;
    private final String claveAdministracion;

    public UsuarioInicializador(UsuarioService usuarioService,
                                @Value("${usuarios.iniciales.admin:}") String claveAdmin,
                                @Value("${usuarios.iniciales.rrhh:}") String claveRrhh,
                                @Value("${usuarios.iniciales.contabilidad:}") String claveContabilidad,
                                @Value("${usuarios.iniciales.administracion:}") String claveAdministracion) {
        this.usuarioService = usuarioService;
        this.claveAdmin = claveAdmin;
        this.claveRrhh = claveRrhh;
        this.claveContabilidad = claveContabilidad;
        this.claveAdministracion = claveAdministracion;
    }

    @Override
    public void run(ApplicationArguments args) {
        crear("admin", claveAdmin, Rol.ADMIN, "ADMIN_PASSWORD");
        crear("rrhh1", claveRrhh, Rol.RRHH, "RRHH_PASSWORD");
        crear("contabilidad1", claveContabilidad, Rol.CONTABILIDAD, "CONTABILIDAD_PASSWORD");
        crear("administracion1", claveAdministracion, Rol.ADMINISTRACION, "ADMINISTRACION_PASSWORD");
    }

    private void crear(String usuario, String clave, Rol rol, String variable) {
        if (clave == null || clave.isBlank()) {
            if (!usuarioService.existe(usuario)) {
                log.warn("No se creó el usuario '{}': falta {} en backend/.env", usuario, variable);
            }
            return;
        }
        usuarioService.crearSiNoExiste(usuario, clave, rol);
    }
}
