package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByProveedorIdAndNumeroComprobante(Long proveedorId, String numeroComprobante);

    @Query("""
            SELECT c
            FROM Compra c
            WHERE c.proveedor.id = :proveedorId
            ORDER BY c.fecha DESC
            """)
    List<Compra> buscarPorProveedor(@Param("proveedorId") Long proveedorId);

    @Query("""
            SELECT c
            FROM Compra c
            WHERE c.fecha BETWEEN :desde AND :hasta
            ORDER BY c.fecha DESC
            """)
    List<Compra> buscarPorPeriodo(@Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COUNT(c)
            FROM Compra c
            WHERE c.fecha BETWEEN :desde AND :hasta
            """)
    Long contarPorPeriodo(@Param("desde") LocalDate desde,
                          @Param("hasta") LocalDate hasta);
}
