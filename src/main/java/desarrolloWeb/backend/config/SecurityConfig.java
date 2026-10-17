package desarrolloWeb.backend.config;

import desarrolloWeb.backend.security.RestAccessDeniedHandler;
import desarrolloWeb.backend.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(List.of("*"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})

            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()

                .requestMatchers("/h2-console", "/h2-console/**")
                    .permitAll()

                .requestMatchers("/api/auth/me")
                    .authenticated()

                // Portal del trabajador: cada uno solo ve y marca lo suyo (el servicio exige la ficha vinculada)
                .requestMatchers("/api/mi", "/api/mi/**")
                    .authenticated()

                // Ubicación de la empresa: la consulta RRHH, la cambia solo el administrador
                .requestMatchers(HttpMethod.GET, "/api/sede")
                    .hasAnyRole("ADMIN", "RRHH")
                .requestMatchers("/api/sede")
                    .hasRole("ADMIN")

                // Proveedor de facturación electrónica activo (solo lectura, sin credenciales)
                .requestMatchers(HttpMethod.GET, "/api/facturacion/estado")
                    .hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION")

                // Contabilidad necesita la lista de trabajadores para registrar pagos
                .requestMatchers(HttpMethod.GET, "/api/personal")
                    .hasAnyRole("ADMIN", "RRHH", "CONTABILIDAD")

                .requestMatchers(
                    "/api/personal", "/api/personal/**",
                    "/api/asistencia", "/api/asistencia/**",
                    "/api/documentos", "/api/documentos/**"
                ).hasAnyRole("ADMIN", "RRHH")

                .requestMatchers(HttpMethod.PATCH, "/api/ventas/*/anular")
                    .hasAnyRole("ADMIN", "CONTABILIDAD")
                .requestMatchers(HttpMethod.GET,
                    "/api/ventas", "/api/ventas/**",
                    "/api/clientes", "/api/clientes/**"
                ).hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION")
                .requestMatchers(
                    "/api/ventas", "/api/ventas/**",
                    "/api/clientes", "/api/clientes/**"
                ).hasAnyRole("ADMIN", "ADMINISTRACION")

                .requestMatchers(HttpMethod.GET,
                    "/api/compras", "/api/compras/**",
                    "/api/proveedores", "/api/proveedores/**"
                ).hasAnyRole("ADMIN", "ADMINISTRACION", "CONTABILIDAD")
                // Contabilidad registra y valida las facturas de compra (crédito fiscal)
                .requestMatchers(
                    "/api/compras", "/api/compras/**",
                    "/api/proveedores", "/api/proveedores/**"
                ).hasAnyRole("ADMIN", "ADMINISTRACION", "CONTABILIDAD")

                .requestMatchers(HttpMethod.PUT, "/api/tributario/configuracion/**")
                    .hasRole("ADMIN")
                .requestMatchers("/api/tributario", "/api/tributario/**")
                    .hasAnyRole("ADMIN", "CONTABILIDAD")

                // Planilla: RRHH la genera, Contabilidad la aprueba (y con eso se crean los pagos)
                .requestMatchers(HttpMethod.PATCH, "/api/planillas/*/aprobar")
                    .hasAnyRole("ADMIN", "CONTABILIDAD")
                .requestMatchers(HttpMethod.GET, "/api/planillas", "/api/planillas/**")
                    .hasAnyRole("ADMIN", "RRHH", "CONTABILIDAD")
                .requestMatchers("/api/planillas", "/api/planillas/**")
                    .hasAnyRole("ADMIN", "RRHH")

                .requestMatchers(
                    "/api/pagos", "/api/pagos/**"
                ).hasAnyRole("ADMIN", "CONTABILIDAD")

                .requestMatchers("/api/**")
                    .hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
