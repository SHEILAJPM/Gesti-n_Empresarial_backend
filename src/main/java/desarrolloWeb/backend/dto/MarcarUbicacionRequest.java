package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/* Lo que envía el celular: su posición GPS, el margen de error que reporta y la foto tomada en ese momento
   (JPEG en Base64 con la ubicación impresa). La hora la pone el servidor. */
public record MarcarUbicacionRequest(
        @NotNull(message = "Falta la latitud") @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @NotNull(message = "Falta la longitud") @DecimalMin("-180") @DecimalMax("180") Double longitud,
        @NotNull(message = "Falta la precisión del GPS") @PositiveOrZero Double precision,
        @NotBlank(message = "Falta la foto: tómate una foto para marcar")
        @Size(max = 900_000, message = "La foto pesa demasiado") String foto) {
}
