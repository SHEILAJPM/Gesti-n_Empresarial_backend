package desarrolloWeb.backend.security;

import desarrolloWeb.backend.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/* Emite el JWT de la sesión: el usuario (sub), su rol y cuándo vence */
@Service
public class TokenService {

    public record Token(String valor, Instant expiraEn) {
    }

    private final JwtEncoder encoder;
    private final Duration duracion;

    public TokenService(JwtEncoder encoder, @Value("${seguridad.jwt.horas:8}") long horas) {
        this.encoder = encoder;
        this.duracion = Duration.ofHours(horas);
    }

    public Token emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gestion-empresarial")
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim("rol", usuario.getRol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Token(encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue(), vence);
    }
}
