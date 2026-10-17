package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraRequest(

        @NotBlank(message = "El RUC del proveedor es obligatorio")
        @Pattern(regexp = "^(10|15|17|20)\\d{9}$",
                message = "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20")
        String ruc,

        @NotBlank(message = "El número de comprobante es obligatorio")
        @Pattern(regexp = "^F\\d{3}-\\d{1,10}$",
                message = "El comprobante de compra debe ser una factura con formato F001-123")
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
