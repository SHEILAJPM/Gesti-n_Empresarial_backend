package desarrolloWeb.backend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Traduce las variables que escribe `neon env pull` al formato de Spring Boot:
   DATABASE_URL (pooled, -pooler)  -> spring.datasource.*  (tráfico normal de la app)
   DATABASE_URL_UNPOOLED (directa) -> spring.flyway.*      (migraciones: PgBouncer no sirve para DDL de sesión)
   Neon entrega postgresql://usuario:clave@host/db?sslmode=require, y JDBC necesita jdbc:postgresql://host/db
   con el usuario y la clave aparte. Si un perfil ya fija spring.datasource.url (local con H2, tests), no hace nada. */
public class NeonDataSourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String FUENTE = "neon-database-url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        String pooled = entorno.getProperty("DATABASE_URL");
        if (entorno.containsProperty("spring.datasource.url") || pooled == null || pooled.isBlank()) {
            return;
        }
        String directa = entorno.getProperty("DATABASE_URL_UNPOOLED", pooled);

        ConexionNeon app = ConexionNeon.desde(pooled);
        ConexionNeon migraciones = ConexionNeon.desde(directa);

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("spring.datasource.url", app.jdbcUrl());
        propiedades.put("spring.datasource.username", app.usuario());
        propiedades.put("spring.datasource.password", app.clave());
        propiedades.put("spring.flyway.url", migraciones.jdbcUrl());
        propiedades.put("spring.flyway.user", migraciones.usuario());
        propiedades.put("spring.flyway.password", migraciones.clave());
        // Al final: cualquier valor configurado explícitamente sigue teniendo prioridad
        entorno.getPropertySources().addLast(new MapPropertySource(FUENTE, propiedades));
    }

    @Override
    public int getOrder() {
        // Después de ConfigDataEnvironmentPostProcessor, para que ya esté cargado backend/.env
        return Ordered.LOWEST_PRECEDENCE;
    }

    record ConexionNeon(String jdbcUrl, String usuario, String clave) {

        static ConexionNeon desde(String url) {
            URI uri = URI.create(url.trim().replaceAll("^\"|\"$", ""));
            if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
                throw new IllegalStateException("DATABASE_URL debe empezar con postgresql://");
            }
            String infoUsuario = uri.getRawUserInfo();
            if (infoUsuario == null || !infoUsuario.contains(":")) {
                throw new IllegalStateException("DATABASE_URL no incluye usuario y contraseña");
            }
            int separador = infoUsuario.indexOf(':');
            String usuario = decodificar(infoUsuario.substring(0, separador));
            String clave = decodificar(infoUsuario.substring(separador + 1));

            String host = uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
            String jdbc = "jdbc:postgresql://" + host + uri.getRawPath() + parametrosJdbc(uri.getRawQuery());
            return new ConexionNeon(jdbc, usuario, clave);
        }

        /* libpq usa channel_binding; el driver JDBC lo llama channelBinding */
        private static String parametrosJdbc(String query) {
            if (query == null || query.isBlank()) {
                return "?sslmode=require";
            }
            List<String> parametros = new ArrayList<>();
            for (String par : query.split("&")) {
                parametros.add(par.startsWith("channel_binding=") ? par.replace("channel_binding=", "channelBinding=") : par);
            }
            return "?" + String.join("&", parametros);
        }

        // En la parte de usuario de una URL, "+" es literal (no un espacio)
        private static String decodificar(String texto) {
            return URLDecoder.decode(texto.replace("+", "%2B"), StandardCharsets.UTF_8);
        }
    }
}
