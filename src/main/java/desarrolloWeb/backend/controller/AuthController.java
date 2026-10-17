package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /* Datos de la sesión: usuario, rol y, si la cuenta pertenece a un trabajador, su id (habilita "Mi espacio") */
    @GetMapping("/me")
    public Map<String, Object> obtenerUsuario(Authentication autenticacion) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario", autenticacion.getName());
        String autoridad = autenticacion.getAuthorities().iterator().next().getAuthority();
        datos.put("rol", autoridad.replace("ROLE_", ""));
        datos.put("trabajadorId", usuarioService.trabajadorDe(autenticacion.getName()));
        return datos;
    }
}
