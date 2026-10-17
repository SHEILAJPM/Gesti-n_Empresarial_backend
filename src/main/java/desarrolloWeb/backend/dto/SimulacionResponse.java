package desarrolloWeb.backend.dto;

import java.math.BigDecimal;

public record SimulacionResponse(
        BigDecimal montoIngresado,
        boolean incluyeIgv,
        BigDecimal tasa,
        BigDecimal base,
        BigDecimal igv,
        BigDecimal total) {
}
