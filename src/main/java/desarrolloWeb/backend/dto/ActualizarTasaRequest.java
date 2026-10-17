package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActualizarTasaRequest(

        @NotNull(message = "La tasa es obligatoria")
        @DecimalMin(value = "0.0000", message = "La tasa no puede ser negativa")
        @DecimalMax(value = "1.0000", message = "La tasa no puede ser mayor a 1 (100 %)")
        @Digits(integer = 1, fraction = 4, message = "La tasa admite hasta 4 decimales, por ejemplo 0.1800")
        BigDecimal tasa) {
}
