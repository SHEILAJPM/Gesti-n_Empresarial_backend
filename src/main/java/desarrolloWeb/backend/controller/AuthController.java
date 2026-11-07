package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.CambiarClaveRequest;
import desarrolloWeb.backend.dto.LoginRequest;
import desarrolloWeb.backend.entity.Usuario;
import desarrolloWeb.backend.repository.UsuarioRepository;
import desarrolloWeb.backend.security.LimiteIntentosLogin;
import desarrolloWeb.backend.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import desarrolloWeb.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LimiteIntentosLogin limiteIntentos;

    public AuthController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                          AuthenticationManager authenticationManager, TokenService tokenService,
                          LimiteIntentosLogin limiteIntentos) {
        this.limiteIntentos = limiteIntentos;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    /* Verifica usuario y contraseña una sola vez y devuelve el JWT que el frontend manda en cada petición
       (Authorization: Bearer ...). Credenciales incorrectas → 401; cuenta desactivada → 401;
       5 fallos seguidos desde la misma IP → 429 durante 15 minutos. */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest datos, HttpServletRequest peticion) {
        String nombre = datos.usuario().trim();
        String ip = peticion.getRemoteAddr();
        limiteIntentos.verificar(nombre, ip);
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(nombre, datos.clave()));
        } catch (AuthenticationException e) {
            limiteIntentos.fallo(nombre, ip);
            throw e;
        }
        limiteIntentos.exito(nombre, ip);
        return conToken(usuarioRepository.findByUsername(autenticacion.getName()).orElseThrow());
    }

    /* El usuario cambia su propia contraseña. Sus otras sesiones se cierran; esta recibe un token nuevo. */
    @PutMapping("/clave")
    public Map<String, Object> cambiarClave(@Valid @RequestBody CambiarClaveRequest datos, Authentication autenticacion) {
        return conToken(usuarioService.cambiarMiClave(autenticacion.getName(), datos.claveActual(), datos.claveNueva()));
    }

    private Map<String, Object> conToken(Usuario usuario) {
        TokenService.Token token = tokenService.emitir(usuario);
        Map<String, Object> respuesta = sesion(usuario.getUsername(), usuario.getRol().name(), usuario.getTrabajadorId());
        respuesta.put("token", token.valor());
        respuesta.put("expiraEn", token.expiraEn().toString());
        return respuesta;
    }

    /* Datos de la sesión: usuario, rol y, si la cuenta pertenece a un trabajador, su id (habilita "Mi espacio") */
    @GetMapping("/me")
    public Map<String, Object> obtenerUsuario(Authentication autenticacion) {
        String autoridad = autenticacion.getAuthorities().iterator().next().getAuthority();
        return sesion(autenticacion.getName(), autoridad.replace("ROLE_", ""),
                usuarioService.trabajadorDe(autenticacion.getName()));
    }

    private static Map<String, Object> sesion(String usuario, String rol, Long trabajadorId) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario", usuario);
        datos.put("rol", rol);
        datos.put("trabajadorId", trabajadorId);
        return datos;
    }
}
