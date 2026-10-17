package desarrolloWeb.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores) {

    public static ErrorResponse de(HttpStatus status, String mensaje, String path) {
        return de(status, mensaje, path, Map.of());
    }

    public static ErrorResponse de(HttpStatus status, String mensaje, String path, Map<String, String> errores) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, path, errores);
    }
}
