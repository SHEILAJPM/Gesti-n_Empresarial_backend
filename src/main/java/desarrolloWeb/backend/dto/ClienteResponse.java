package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoDocumento;

public record ClienteResponse(Long id, String documento, String nombre, TipoDocumento tipoDocumento) {
}
