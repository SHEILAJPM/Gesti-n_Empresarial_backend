package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.ModalidadTraslado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* Guía de remisión remitente. Con ventaId, el destinatario y los bienes salen de la venta si no se envían.
   Transporte PUBLICO: RUC y razón social del transportista. PRIVADO: placa, DNI, nombre y licencia del conductor. */
public record GuiaRequest(
        Long ventaId,
        @NotBlank(message = "Elige el motivo del traslado") String motivoCodigo,
        LocalDate fechaTraslado,
        @Pattern(regexp = "^$|^(\\d{8}|\\d{11})$", message = "El destinatario debe tener DNI (8) o RUC (11 dígitos)")
        String destinatarioDocumento,
        @Size(max = 150, message = "Hasta 150 caracteres") String destinatarioNombre,
        @NotNull(message = "Indica el peso bruto")
        @DecimalMin(value = "0.001", message = "El peso debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3, message = "El peso admite hasta 3 decimales") BigDecimal pesoBrutoKg,
        @Positive(message = "Los bultos deben ser mayores a cero") Integer numeroBultos,
        @NotNull(message = "Elige el tipo de transporte") ModalidadTraslado modalidad,
        @Pattern(regexp = "^$|^\\d{11}$", message = "El RUC del transportista tiene 11 dígitos") String transportistaRuc,
        @Size(max = 150) String transportistaRazonSocial,
        @Pattern(regexp = "^$|^[A-Za-z0-9]{3}-?[A-Za-z0-9]{3}$", message = "Placa con formato ABC-123") String vehiculoPlaca,
        @Pattern(regexp = "^$|^\\d{8}$", message = "El DNI del conductor tiene 8 dígitos") String conductorDni,
        @Size(max = 150) String conductorNombres,
        @Size(max = 15) String conductorLicencia,
        @NotBlank(message = "Falta el ubigeo de partida")
        @Pattern(regexp = "^\\d{6}$", message = "El ubigeo tiene 6 dígitos") String partidaUbigeo,
        @NotBlank(message = "Falta la dirección de partida") @Size(max = 200) String partidaDireccion,
        @NotBlank(message = "Falta el ubigeo de llegada")
        @Pattern(regexp = "^\\d{6}$", message = "El ubigeo tiene 6 dígitos") String llegadaUbigeo,
        @NotBlank(message = "Falta la dirección de llegada") @Size(max = 200) String llegadaDireccion,
        @Size(max = 250) String observaciones,
        @Valid @Size(max = 100) List<Item> items) {

    public record Item(
            @Size(max = 30) String codigo,
            @NotBlank(message = "Cada bien necesita una descripción") @Size(max = 250) String descripcion,
            @NotBlank(message = "Cada bien necesita una unidad") String unidad,
            @NotNull(message = "Cada bien necesita una cantidad")
            @Positive(message = "La cantidad debe ser mayor a cero")
            @Digits(integer = 9, fraction = 3) BigDecimal cantidad) {
    }
}
