package desarrolloWeb.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class NeonDataSourceEnvironmentPostProcessorTest {

    private final NeonDataSourceEnvironmentPostProcessor procesador = new NeonDataSourceEnvironmentPostProcessor();

    @Test
    void conviertePooledParaLaAppYDirectaParaFlyway() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("DATABASE_URL",
                        "postgresql://neondb_owner:npg_a%2Fb+c@ep-uno-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require")
                .withProperty("DATABASE_URL_UNPOOLED",
                        "postgresql://neondb_owner:npg_a%2Fb+c@ep-uno.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

        procesador.postProcessEnvironment(entorno, new SpringApplication());

        assertThat(entorno.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-uno-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require&channelBinding=require");
        assertThat(entorno.getProperty("spring.datasource.username")).isEqualTo("neondb_owner");
        assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("npg_a/b+c");
        assertThat(entorno.getProperty("spring.flyway.url"))
                .isEqualTo("jdbc:postgresql://ep-uno.us-east-2.aws.neon.tech/neondb?sslmode=require&channelBinding=require");
        assertThat(entorno.getProperty("spring.flyway.user")).isEqualTo("neondb_owner");
    }

    @Test
    void respetaUnaUrlConfiguradaExplicitamente() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:local")
                .withProperty("DATABASE_URL", "postgresql://u:p@ep-uno-pooler.aws.neon.tech/neondb");

        procesador.postProcessEnvironment(entorno, new SpringApplication());

        assertThat(entorno.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:local");
        assertThat(entorno.getProperty("spring.flyway.url")).isNull();
    }

    @Test
    void sinDatabaseUrlNoTocaNada() {
        MockEnvironment entorno = new MockEnvironment();

        procesador.postProcessEnvironment(entorno, new SpringApplication());

        assertThat(entorno.getProperty("spring.datasource.url")).isNull();
    }
}
