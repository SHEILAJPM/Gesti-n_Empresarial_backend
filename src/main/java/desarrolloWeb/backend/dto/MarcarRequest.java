package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoMarcacion;
import java.time.LocalDateTime;

public record MarcarRequest(
        String documentoIdentidad,
        TipoMarcacion tipo,
        LocalDateTime fechaHora
) {
}