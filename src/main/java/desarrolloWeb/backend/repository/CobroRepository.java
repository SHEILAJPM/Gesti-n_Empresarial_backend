package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Cobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CobroRepository extends JpaRepository<Cobro, Long> {

    List<Cobro> findByVentaIdOrderByFechaAscIdAsc(Long ventaId);

    @Query("SELECT COALESCE(SUM(c.monto), 0) FROM Cobro c WHERE c.venta.id = :ventaId")
    BigDecimal totalPorVenta(@Param("ventaId") Long ventaId);

    /* [ventaId, total cobrado] de todas las ventas con cobros: una sola consulta para el listado */
    @Query("SELECT c.venta.id, SUM(c.monto) FROM Cobro c GROUP BY c.venta.id")
    List<Object[]> totalesPorVenta();

    @Query("SELECT COALESCE(SUM(c.monto), 0) FROM Cobro c WHERE c.fecha BETWEEN :desde AND :hasta")
    BigDecimal totalEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
