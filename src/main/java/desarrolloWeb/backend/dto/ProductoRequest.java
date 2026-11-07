package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/* valorUnitario: precio SIN IGV. unidad: código del catálogo 03 de SUNAT (ver UnidadMedida). */
public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,30}$", message = "El código admite hasta 30 letras, números, punto o guion")
        String codigo,
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 250, message = "La descripción no puede superar 250 caracteres")
        String descripcion,
        @NotBlank(message = "La unidad es obligatoria")
        String unidad,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal valorUnitario) {
}
