package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(

        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$",
                message = "El documento debe ser un DNI de 8 dígitos o un RUC de 11 dígitos")
        String documento,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre) {
}
