package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(

        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "^(10|15|17|20)\\d{9}$",
                message = "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20")
        String ruc,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar 150 caracteres")
        String razonSocial,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion) {
}
