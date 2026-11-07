package desarrolloWeb.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/* Firma y verificación de los JWT con una clave secreta (HS256), leída de JWT_SECRET en .env.
   Sin clave (o muy corta) se genera una al azar: funciona, pero las sesiones se cierran al reiniciar el servidor. */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    static final int LARGO_MINIMO = 32;

    private final SecretKey clave;

    public JwtConfig(@Value("${seguridad.jwt.secreto:}") String secreto) {
        byte[] bytes;
        if (secreto == null || secreto.length() < LARGO_MINIMO) {
            log.warn("JWT_SECRET no está definido o tiene menos de {} caracteres: se usa una clave temporal y las "
                    + "sesiones se cerrarán al reiniciar el servidor", LARGO_MINIMO);
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            bytes = Base64.getEncoder().encode(bytes);
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
