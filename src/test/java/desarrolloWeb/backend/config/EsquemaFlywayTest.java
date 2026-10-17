package desarrolloWeb.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/* Aplica las migraciones Flyway (las mismas que corren en Neon) y deja que Hibernate valide
   las entidades contra ese esquema: si una entidad cambia sin su migración, este test falla. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywaydb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/datos-ejemplo",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class EsquemaFlywayTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migracionesCoincidenConLasEntidadesYCarganDatosDeEjemplo() {
        Integer ventas = jdbc.queryForObject("SELECT COUNT(*) FROM ventas", Integer.class);
        Integer usuarios = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);

        assertThat(ventas).isEqualTo(4);
        assertThat(usuarios).isEqualTo(4);
    }
}
