package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoComprobante;

public record VentasPorComprobante(TipoComprobante tipoComprobante, Long cantidad) {
}
