package desarrolloWeb.backend.config;

import desarrolloWeb.backend.security.ConversorJwt;
import desarrolloWeb.backend.security.RestAccessDeniedHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import desarrolloWeb.backend.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
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

    /* Solo los orígenes de cors.origenes (CORS_ORIGENES en .env, separados por coma) pueden llamar a la API desde un navegador */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.origenes:http://localhost:4200}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(origenes.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    /* El login usa el mismo mecanismo que HTTP Basic: UsuarioService + BCrypt */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    /* Dos formas de autenticarse:
       - Bearer <JWT>: la que usa el frontend (POST /api/auth/login). El navegador no guarda la contraseña.
       - HTTP Basic: para Postman, curl y los tests. */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            ConversorJwt conversorJwt) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(recurso -> recurso
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt))
                .authenticationEntryPoint(authenticationEntryPoint))

            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()

                .requestMatchers("/h2-console", "/h2-console/**")
                    .permitAll()

                // Inicio de sesión: recibe usuario y contraseña y devuelve el JWT
                .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()

                .requestMatchers("/api/auth/me", "/api/auth/clave")
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
                    .hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION", "VENDEDOR", "ALMACEN")

                // Autocompletar nombre o razón social al registrar clientes, proveedores o trabajadores
                .requestMatchers(HttpMethod.GET, "/api/consultas/**")
                    .hasAnyRole("ADMIN", "RRHH", "CONTABILIDAD", "ADMINISTRACION", "VENDEDOR")

                // Contabilidad necesita la lista de trabajadores para registrar pagos
                .requestMatchers(HttpMethod.GET, "/api/personal")
                    .hasAnyRole("ADMIN", "RRHH", "CONTABILIDAD")

                .requestMatchers(
                    "/api/personal", "/api/personal/**",
                    "/api/asistencia", "/api/asistencia/**",
                    "/api/documentos", "/api/documentos/**"
                ).hasAnyRole("ADMIN", "RRHH")

                // Catálogo de productos y servicios: lo consulta quien factura o revisa ventas, lo edita quien factura
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**")
                    .hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION", "VENDEDOR", "ALMACEN")
                .requestMatchers("/api/productos", "/api/productos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRACION")

                // Guías de remisión: las emiten Administración, Almacén y los vendedores; Contabilidad las consulta
                .requestMatchers(HttpMethod.GET, "/api/guias", "/api/guias/**")
                    .hasAnyRole("ADMIN", "ADMINISTRACION", "CONTABILIDAD", "ALMACEN", "VENDEDOR")
                .requestMatchers("/api/guias", "/api/guias/**")
                    .hasAnyRole("ADMIN", "ADMINISTRACION", "ALMACEN", "VENDEDOR")

                // Cuentas por cobrar: Contabilidad registra los cobros; Administración consulta
                .requestMatchers(HttpMethod.GET, "/api/cobranzas", "/api/cobranzas/**")
                    .hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION")
                .requestMatchers("/api/cobranzas", "/api/cobranzas/**")
                    .hasAnyRole("ADMIN", "CONTABILIDAD")

                // Notas de crédito y débito: las emite quien factura y también Contabilidad (corrige comprobantes)
                .requestMatchers("/api/notas", "/api/notas/**")
                    .hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION")

                .requestMatchers(HttpMethod.PATCH, "/api/ventas/*/anular")
                    .hasAnyRole("ADMIN", "CONTABILIDAD")
                .requestMatchers(HttpMethod.GET,
                    "/api/ventas", "/api/ventas/**",
                    "/api/clientes", "/api/clientes/**"
                ).hasAnyRole("ADMIN", "CONTABILIDAD", "ADMINISTRACION", "VENDEDOR", "ALMACEN")
                // El vendedor emite boletas y facturas (y reintenta el envío a SUNAT) y registra clientes nuevos;
                // editar o borrar clientes y anular ventas queda para Administración/Contabilidad
                .requestMatchers(HttpMethod.POST, "/api/ventas", "/api/ventas/*/sunat/reenviar", "/api/clientes")
                    .hasAnyRole("ADMIN", "ADMINISTRACION", "VENDEDOR")
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
