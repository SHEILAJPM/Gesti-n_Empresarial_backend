
package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Marcacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MarcacionRepository
        extends JpaRepository<Marcacion, Long> {

    List<Marcacion> findByTrabajadorId(Long trabajadorId);

    List<Marcacion> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Marcacion> findByTrabajadorIdAndFechaHoraBetweenOrderByFechaHoraAsc(
            Long trabajadorId, LocalDateTime desde, LocalDateTime hasta);

    boolean existsByTrabajadorId(Long trabajadorId);

    @Query("SELECT m FROM Marcacion m WHERE m.trabajadorId = :trabajadorId ORDER BY m.fechaHora DESC")
    List<Marcacion> buscarPorTrabajadorJPQL(
            @Param("trabajadorId") Long trabajadorId);
}
