package desarrolloWeb.backend.security;

import desarrolloWeb.backend.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse error = ErrorResponse.de(HttpStatus.UNAUTHORIZED,
                "Credenciales ausentes o incorrectas; envíe Authorization: Basic",
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // El frontend envía X-Requested-With; sin este encabezado el navegador abriría su ventana nativa de login
        if (request.getHeader("X-Requested-With") == null) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"desarrollo-web\"");
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(error));
    }
}
