package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByEstado(String estado);
    List<Pago> findByTrabajador_Id(Long trabajadorId);
    List<Pago> findByProveedor_Id(Long proveedorId);
    boolean existsByProveedor_Id(Long proveedorId);

    boolean existsByTrabajador_Id(Long trabajadorId);
}
