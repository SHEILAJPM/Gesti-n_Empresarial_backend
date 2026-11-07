package desarrolloWeb.backend.security;

import desarrolloWeb.backend.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Límite de intentos de inicio de sesión")
class LimiteIntentosLoginTest {

    private static final Instant T0 = Instant.parse("2026-11-06T15:00:00Z");

    @Test
    void cincoFallosBloqueanQuinceMinutos() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(Clock.fixed(T0, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) {
            limite.fallo("Admin", "1.2.3.4");
        }
        DemasiadosIntentosException e = assertThrows(DemasiadosIntentosException.class,
                () -> limite.verificar("admin", "1.2.3.4"));
        assertTrue(e.getMessage().contains("15 minutos"), e.getMessage());

        // Desde otra IP el mismo usuario sigue pudiendo intentar
        assertDoesNotThrow(() -> limite.verificar("admin", "5.6.7.8"));
    }

    @Test
    void elBloqueoTerminaPasadoElPlazo() {
        Clock[] reloj = {Clock.fixed(T0, ZoneOffset.UTC)};
        LimiteIntentosLogin limite = new LimiteIntentosLogin(new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zona) { return this; }
            @Override public Instant instant() { return reloj[0].instant(); }
        });
        for (int i = 0; i < 5; i++) {
            limite.fallo("admin", "1.2.3.4");
        }
        reloj[0] = Clock.fixed(T0.plus(Duration.ofMinutes(16)), ZoneOffset.UTC);
        assertDoesNotThrow(() -> limite.verificar("admin", "1.2.3.4"));
    }

    @Test
    void unAccesoCorrectoReiniciaElConteo() {
        LimiteIntentosLogin limite = new LimiteIntentosLogin(Clock.fixed(T0, ZoneOffset.UTC));
        for (int i = 0; i < 4; i++) {
            limite.fallo("rrhh1", "1.2.3.4");
        }
        limite.exito("rrhh1", "1.2.3.4");
        limite.fallo("rrhh1", "1.2.3.4");
        assertDoesNotThrow(() -> limite.verificar("rrhh1", "1.2.3.4"));
    }
}
