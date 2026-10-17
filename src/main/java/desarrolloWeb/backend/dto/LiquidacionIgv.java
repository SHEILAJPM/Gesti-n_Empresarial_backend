package desarrolloWeb.backend.dto;

import java.math.BigDecimal;

public record LiquidacionIgv(BigDecimal igvPorPagar, BigDecimal saldoAFavor) {
}
