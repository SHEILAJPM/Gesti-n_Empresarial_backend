package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Planilla;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanillaRepository extends JpaRepository<Planilla, Long> {

    Optional<Planilla> findByAnioAndMes(Integer anio, Integer mes);

    /* SELECT ... FOR UPDATE: si dos personas aprueban a la vez, la segunda espera y ya ve la planilla APROBADA */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Planilla p where p.id = :id")
    Optional<Planilla> buscarParaActualizar(@Param("id") Long id);

    List<Planilla> findAllByOrderByAnioDescMesDesc();
}
