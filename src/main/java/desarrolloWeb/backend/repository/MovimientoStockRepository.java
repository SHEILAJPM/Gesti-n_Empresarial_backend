package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /* Kárdex de un producto, del más reciente al más antiguo */
    List<MovimientoStock> findTop500ByProductoIdOrderByFechaDescIdDesc(Long productoId);

    /* Lo que una venta sacó y lo que ya volvió (anulación o notas de crédito) */
    List<MovimientoStock> findByVentaId(Long ventaId);

    boolean existsByCompraId(Long compraId);

    /* Fecha del último movimiento de cada producto: [productoId, fecha] */
    @Query("SELECT m.producto.id, MAX(m.fecha) FROM MovimientoStock m GROUP BY m.producto.id")
    List<Object[]> ultimoMovimientoPorProducto();

    /* Compras que ya tienen un ingreso al almacén */
    @Query("SELECT DISTINCT m.compra.id FROM MovimientoStock m WHERE m.compra.id IN :ids")
    List<Long> comprasIngresadas(@Param("ids") Collection<Long> ids);
}
