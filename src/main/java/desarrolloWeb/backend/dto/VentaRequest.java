package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* Venta con detalle (items) o, como antes, solo con el monto sin IGV (una línea genérica).
   Si llegan items, el monto se calcula de ellos y montoSinIgv se ignora. */
public record VentaRequest(

        @NotBlank(message = "El documento del cliente es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$",
                message = "El documento debe ser un DNI de 8 dígitos o un RUC de 11 dígitos")
        String documentoCliente,

        @NotBlank(message = "El número de comprobante es obligatorio")
        @Pattern(regexp = "^[FB]\\d{3}-\\d{1,10}$",
                message = "El comprobante debe tener el formato F001-123 o B001-123")
        String numeroComprobante,

        @Positive(message = "El monto sin IGV debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2,
                message = "El monto admite hasta 10 enteros y 2 decimales")
        BigDecimal montoSinIgv,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @Valid
        @Size(max = 100, message = "Un comprobante admite hasta 100 líneas")
        List<Item> items,

        FormaPago formaPago,

        @Valid
        @Size(max = 36, message = "Hasta 36 cuotas")
        List<Cuota> cuotas,

        @Size(max = 250, message = "Las observaciones no pueden superar 250 caracteres")
        String observaciones) {

    /* Venta sin detalle, al contado (forma anterior) */
    public VentaRequest(String documentoCliente, String numeroComprobante, BigDecimal montoSinIgv, LocalDate fecha) {
        this(documentoCliente, numeroComprobante, montoSinIgv, fecha, null, null, null, null);
    }

    /* productoId es opcional: sin él es una línea escrita a mano */
    public record Item(
            Long productoId,
            @Size(max = 30, message = "El código admite hasta 30 caracteres") String codigo,
            @NotBlank(message = "Cada línea necesita una descripción")
            @Size(max = 250, message = "La descripción no puede superar 250 caracteres") String descripcion,
            @NotBlank(message = "Cada línea necesita una unidad") String unidad,
            @NotNull(message = "Cada línea necesita una cantidad")
            @Positive(message = "La cantidad debe ser mayor a cero")
            @Digits(integer = 9, fraction = 3, message = "La cantidad admite hasta 3 decimales") BigDecimal cantidad,
            @NotNull(message = "Cada línea necesita un precio")
            @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 2 decimales") BigDecimal valorUnitario) {
    }

    public record Cuota(
            @NotNull(message = "Cada cuota necesita su fecha de pago") LocalDate fechaVencimiento,
            @NotNull(message = "Cada cuota necesita su monto")
            @Positive(message = "El monto de la cuota debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal monto) {
    }
}
