package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Producto;

import java.math.BigDecimal;

public record ProductoResponse(Long id, String codigo, String descripcion, String unidad, BigDecimal valorUnitario,
                               boolean activo) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getDescripcion(), p.getUnidad(), p.getValorUnitario(),
                p.isActivo());
    }
}
