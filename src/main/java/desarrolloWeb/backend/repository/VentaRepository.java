package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    boolean existsByNumeroComprobante(String numeroComprobante);

    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.cliente.id = :clienteId
            ORDER BY v.fecha DESC
            """)
    List<Venta> buscarPorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.fecha BETWEEN :desde AND :hasta
            ORDER BY v.fecha DESC
            """)
    List<Venta> buscarPorPeriodo(@Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT v
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fecha BETWEEN :desde AND :hasta
            ORDER BY v.fecha DESC
            """)
    List<Venta> buscarPorEstadoYPeriodo(@Param("estado") EstadoVenta estado,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fecha BETWEEN :desde AND :hasta
            """)
    Long contarPorEstadoYPeriodo(@Param("estado") EstadoVenta estado,
                                 @Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.tipoComprobante = :tipo
              AND v.estado = :estado
            """)
    Long contarPorTipoComprobante(@Param("tipo") TipoComprobante tipo,
                                  @Param("estado") EstadoVenta estado);

    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.cliente.id = :clienteId
            """)
    Long contarPorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT AVG(v.montoSinIgv)
            FROM Venta v
            WHERE v.estado = :estado
              AND v.fecha BETWEEN :desde AND :hasta
            """)
    Double promedioMontoSinIgv(@Param("estado") EstadoVenta estado,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);
}
