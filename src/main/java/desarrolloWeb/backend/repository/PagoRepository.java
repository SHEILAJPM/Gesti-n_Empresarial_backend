package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /* SELECT ... FOR UPDATE: un pago no se marca como pagado dos veces aunque lleguen dos peticiones juntas */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> buscarParaActualizar(@Param("id") Long id);

    List<Pago> findByEstado(String estado);
    List<Pago> findByTrabajador_Id(Long trabajadorId);
    List<Pago> findByProveedor_Id(Long proveedorId);
    boolean existsByProveedor_Id(Long proveedorId);

    boolean existsByTrabajador_Id(Long trabajadorId);
}
