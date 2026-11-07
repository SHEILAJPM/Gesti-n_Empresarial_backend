package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Nota;
import desarrolloWeb.backend.entity.TipoNota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    @Query("SELECT n FROM Nota n JOIN FETCH n.venta v JOIN FETCH v.cliente ORDER BY n.fecha DESC, n.id DESC")
    List<Nota> listarConVenta();

    List<Nota> findByVentaIdOrderByIdAsc(Long ventaId);

    /* Total de las notas de un tipo sobre una venta (para el saldo del comprobante) */
    @Query("SELECT COALESCE(SUM(n.montoTotal), 0) FROM Nota n WHERE n.venta.id = :ventaId AND n.tipo = :tipo")
    BigDecimal totalPorVentaYTipo(@Param("ventaId") Long ventaId, @Param("tipo") TipoNota tipo);

    /* Notas del periodo sobre ventas vigentes: ajustan el débito fiscal del resumen tributario */
    @Query("SELECT n FROM Nota n WHERE n.fecha BETWEEN :desde AND :hasta AND n.venta.estado = :estado")
    List<Nota> buscarPorPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                @Param("estado") EstadoVenta estado);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(numero, :desde) AS BIGINT)) FROM notas "
            + "WHERE numero LIKE CONCAT(:prefijo, '%')", nativeQuery = true)
    Long maximoCorrelativo(@Param("prefijo") String prefijo, @Param("desde") int desde);
}
