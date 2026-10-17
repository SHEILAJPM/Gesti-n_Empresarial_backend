package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaRequest(

        @NotBlank(message = "El documento del cliente es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$",
                message = "El documento debe ser un DNI de 8 dígitos o un RUC de 11 dígitos")
        String documentoCliente,

        @NotBlank(message = "El número de comprobante es obligatorio")
        @Pattern(regexp = "^[FB]\\d{3}-\\d{1,10}$",
                message = "El comprobante debe tener el formato F001-123 o B001-123")
        String numeroComprobante,

        @NotNull(message = "El monto sin IGV es obligatorio")
        @Positive(message = "El monto sin IGV debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2,
                message = "El monto admite hasta 10 enteros y 2 decimales")
        BigDecimal montoSinIgv,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha) {
}
