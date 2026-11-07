package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* claveActual solo se exige cuando el usuario cambia la suya; el administrador que restablece otra no la envía */
public record CambiarClaveRequest(
        String claveActual,
        @NotBlank(message = "Escribe la nueva contraseña")
        @Size(max = 72, message = "La contraseña no puede superar 72 caracteres")
        String claveNueva) {
}
