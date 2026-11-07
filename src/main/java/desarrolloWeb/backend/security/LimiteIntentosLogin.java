package desarrolloWeb.backend.security;

import desarrolloWeb.backend.exception.DemasiadosIntentosException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* Frena a quien prueba contraseñas: tras 5 fallos seguidos del mismo usuario desde la misma IP,
   el login queda bloqueado 15 minutos. Se guarda en memoria (se reinicia con el servidor). */
@Component
public class LimiteIntentosLogin {

    static final int MAXIMO_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant bloqueadoHasta) {
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteIntentosLogin() {
        this(Clock.systemUTC());
    }

    LimiteIntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    public void verificar(String usuario, String ip) {
        Registro r = registros.get(clave(usuario, ip));
        if (r != null && r.bloqueadoHasta() != null) {
            Duration resta = Duration.between(Instant.now(reloj), r.bloqueadoHasta());
            if (!resta.isNegative()) {
                long minutos = Math.max(1, (resta.toSeconds() + 59) / 60);
                throw new DemasiadosIntentosException("Demasiados intentos fallidos. Vuelve a intentarlo en "
                        + minutos + (minutos == 1 ? " minuto." : " minutos."));
            }
            registros.remove(clave(usuario, ip));
        }
    }

    public void fallo(String usuario, String ip) {
        registros.compute(clave(usuario, ip), (k, r) -> {
            int fallos = (r == null ? 0 : r.fallos()) + 1;
            return new Registro(fallos, fallos >= MAXIMO_FALLOS ? Instant.now(reloj).plus(BLOQUEO) : null);
        });
    }

    public void exito(String usuario, String ip) {
        registros.remove(clave(usuario, ip));
    }

    private static String clave(String usuario, String ip) {
        return usuario.trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }
}
