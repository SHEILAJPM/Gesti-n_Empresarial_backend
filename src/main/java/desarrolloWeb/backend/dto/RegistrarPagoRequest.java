package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarPagoRequest(
        String tipoBeneficiario,
        String documentoIdentidad,
        String rucProveedor,
        String concepto,
        BigDecimal monto,
        LocalDate fechaProgramada) {
}
