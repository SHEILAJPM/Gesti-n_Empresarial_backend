package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoMarcacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IntentoMarcacionResponse(Long id, Long trabajadorId, String nombres, LocalDateTime fechaHora,
                                       TipoMarcacion tipo, BigDecimal distanciaMetros, BigDecimal precisionMetros,
                                       BigDecimal latitud, BigDecimal longitud, String motivo) {
}
