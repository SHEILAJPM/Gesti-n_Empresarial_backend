package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CesarTrabajadorRequest(
        @NotNull(message = "La fecha de cese es obligatoria")
        LocalDate fechaCese,

        @NotBlank(message = "El motivo del cese es obligatorio")
        @Size(max = 250, message = "El motivo no puede superar 250 caracteres")
        String motivo) {
}
