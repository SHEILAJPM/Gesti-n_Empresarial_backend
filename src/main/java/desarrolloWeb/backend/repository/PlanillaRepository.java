package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Planilla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanillaRepository extends JpaRepository<Planilla, Long> {

    Optional<Planilla> findByAnioAndMes(Integer anio, Integer mes);

    List<Planilla> findAllByOrderByAnioDescMesDesc();
}
