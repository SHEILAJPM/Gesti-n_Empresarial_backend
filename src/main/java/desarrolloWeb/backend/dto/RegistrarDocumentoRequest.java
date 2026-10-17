package desarrolloWeb.backend.dto;

import java.time.LocalDate;

public record RegistrarDocumentoRequest(String documentoIdentidad, String tipo, LocalDate fechaVencimiento) {
}
