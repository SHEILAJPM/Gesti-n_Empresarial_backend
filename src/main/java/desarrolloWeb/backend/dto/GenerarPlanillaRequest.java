package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerarPlanillaRequest(
        @NotNull(message = "El año es obligatorio")
        @Min(value = 2000, message = "El año no es válido")
        @Max(value = 2100, message = "El año no es válido")
        Integer anio,

        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        Integer mes) {
}
