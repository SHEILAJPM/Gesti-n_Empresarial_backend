package desarrolloWeb.backend.dto;

import java.time.LocalDate;

public record MiBoletaResponse(Integer anio, Integer mes, LocalDate fechaPago, PlanillaResponse.Detalle detalle) {
}
