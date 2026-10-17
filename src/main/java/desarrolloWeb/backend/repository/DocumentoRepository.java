
package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentoRepository
        extends JpaRepository<Documento, Long> {

    List<Documento> findByTrabajadorId(Long trabajadorId);

    boolean existsByTrabajadorId(Long trabajadorId);

    @Query("SELECT d FROM Documento d WHERE d.trabajadorId = :trabajadorId ORDER BY d.fechaVencimiento ASC")
    List<Documento> buscarPorTrabajadorJPQL(
            @Param("trabajadorId") Long trabajadorId);
}
