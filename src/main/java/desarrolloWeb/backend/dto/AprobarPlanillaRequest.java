package desarrolloWeb.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AprobarPlanillaRequest(
        @NotNull(message = "La fecha de pago es obligatoria")
        LocalDate fechaPago) {
}
