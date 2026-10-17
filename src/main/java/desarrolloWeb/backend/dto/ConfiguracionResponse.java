package desarrolloWeb.backend.dto;

import java.math.BigDecimal;

public record ConfiguracionResponse(String codigo, BigDecimal tasa, boolean vigente) {
}
