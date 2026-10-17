package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SedeRequest(
        @NotBlank(message = "El nombre de la sede es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "Latitud no válida")
        @DecimalMax(value = "90", message = "Latitud no válida")
        BigDecimal latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "Longitud no válida")
        @DecimalMax(value = "180", message = "Longitud no válida")
        BigDecimal longitud,

        @NotNull(message = "El radio es obligatorio")
        @Min(value = 5, message = "El radio mínimo es 5 m")
        @Max(value = 1000, message = "El radio máximo es 1000 m")
        Integer radioMetros,

        @NotNull(message = "La precisión máxima es obligatoria")
        @Min(value = 5, message = "La precisión mínima es 5 m")
        @Max(value = 500, message = "La precisión máxima es 500 m")
        Integer precisionMaxima) {
}
