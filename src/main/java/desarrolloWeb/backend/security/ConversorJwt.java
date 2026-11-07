package desarrolloWeb.backend.security;

import desarrolloWeb.backend.entity.Usuario;
import desarrolloWeb.backend.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/* Convierte el JWT en la sesión del usuario. El rol y el estado se leen de la base en cada petición:
   si el administrador desactiva una cuenta o le cambia el rol, el cambio vale de inmediato aunque el token siga vigente. */
@Component
public class ConversorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public ConversorJwt(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarioRepository.findByUsername(jwt.getSubject())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new DisabledException("La cuenta ya no está activa"));
        // Si cambió la contraseña después de emitirse este token, la sesión se cierra (p. ej. en otro dispositivo)
        LocalDateTime cambio = usuario.getClaveCambiadaEn();
        if (cambio != null && jwt.getIssuedAt() != null
                && LocalDateTime.ofInstant(jwt.getIssuedAt(), ZoneOffset.UTC).isBefore(cambio)) {
            throw new CredentialsExpiredException("La contraseña cambió; vuelve a iniciar sesión");
        }
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())),
                usuario.getUsername());
    }
}
