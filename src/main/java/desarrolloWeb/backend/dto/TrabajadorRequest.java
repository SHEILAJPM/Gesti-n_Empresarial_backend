package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/* Al actualizar, documentoIdentidad se ignora: el documento de un trabajador no cambia */
public record TrabajadorRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150, message = "Los nombres no pueden superar 150 caracteres")
        String nombres,

        @NotBlank(message = "El documento de identidad es obligatorio")
        @Pattern(regexp = "^(\\d{8}|[A-Za-z0-9]{9,12})$",
                message = "El documento debe ser un DNI de 8 dígitos o un carné de extranjería de 9 a 12 caracteres")
        String documentoIdentidad,

        @NotBlank(message = "El cargo es obligatorio")
        @Size(max = 100, message = "El cargo no puede superar 100 caracteres")
        String cargo,

        @NotBlank(message = "El área es obligatoria")
        @Size(max = 100, message = "El área no puede superar 100 caracteres")
        String area,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        LocalDate fechaIngreso,

        @NotNull(message = "El sueldo es obligatorio")
        @PositiveOrZero(message = "El sueldo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El sueldo admite hasta 10 enteros y 2 decimales")
        BigDecimal sueldo,

        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @Pattern(regexp = "^$|^\\+?[0-9 ]{6,20}$", message = "El teléfono solo admite números, espacios y +")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correo,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion,

        @Pattern(regexp = "^$|^(ONP|AFP)$", message = "El sistema de pensiones debe ser ONP o AFP")
        String sistemaPension,

        @Size(max = 30, message = "El nombre de la AFP no puede superar 30 caracteres")
        String afp,

        @Size(max = 60, message = "El banco no puede superar 60 caracteres")
        String banco,

        @Pattern(regexp = "^$|^[0-9-]{6,30}$", message = "El número de cuenta solo admite dígitos y guiones")
        String numeroCuenta) {
}
