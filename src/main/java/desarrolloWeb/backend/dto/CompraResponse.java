package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraResponse(
        Long id,
        Long proveedorId,
        String rucProveedor,
        String razonSocialProveedor,
        String numeroComprobante,
        LocalDate fecha,
        BigDecimal montoSinIgv,
        BigDecimal igv,
        BigDecimal montoTotal,
        BigDecimal tasaAplicada) {
}
