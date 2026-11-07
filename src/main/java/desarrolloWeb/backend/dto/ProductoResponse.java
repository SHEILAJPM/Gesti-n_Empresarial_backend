package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Producto;

import java.math.BigDecimal;

/* manejaStock = false para servicios (unidad ZZ u HUR): su stock no se controla */
public record ProductoResponse(Long id, String codigo, String descripcion, String unidad, BigDecimal valorUnitario,
                               boolean activo, boolean manejaStock, BigDecimal stock, BigDecimal stockMinimo) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getDescripcion(), p.getUnidad(), p.getValorUnitario(),
                p.isActivo(), p.manejaStock(), p.getStock(), p.getStockMinimo());
    }
}
