package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenTributario(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalComprasSinIgv,
        BigDecimal igvCompras,
        BigDecimal totalVentasSinIgv,
        BigDecimal igvVentas,
        BigDecimal igvPorPagar,
        BigDecimal saldoAFavor,
        long cantidadCompras,
        long cantidadVentas) {
}
