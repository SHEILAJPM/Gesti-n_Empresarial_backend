package desarrolloWeb.backend.dto;

import java.time.LocalDate;

public record RegistrarPagoRequest(
        String tipoBeneficiario,
        String documentoIdentidad,
        String rucProveedor,
        String concepto,
        Double monto,
        LocalDate fechaProgramada) {
}
