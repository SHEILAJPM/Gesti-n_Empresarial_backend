package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/* Lo que envía el celular: su posición GPS y el margen de error que reporta. La hora la pone el servidor. */
public record MarcarUbicacionRequest(
        @NotNull(message = "Falta la latitud") @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @NotNull(message = "Falta la longitud") @DecimalMin("-180") @DecimalMax("180") Double longitud,
        @NotNull(message = "Falta la precisión del GPS") @PositiveOrZero Double precision) {
}
