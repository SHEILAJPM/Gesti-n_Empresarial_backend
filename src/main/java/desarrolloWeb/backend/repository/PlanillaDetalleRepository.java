package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.PlanillaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Long> {

    /* Boletas del trabajador en planillas aprobadas, de la más reciente a la más antigua */
    @Query("SELECT d FROM PlanillaDetalle d JOIN FETCH d.planilla p WHERE d.trabajadorId = :trabajadorId "
            + "AND p.estado = :estado ORDER BY p.anio DESC, p.mes DESC")
    List<PlanillaDetalle> buscarBoletas(@Param("trabajadorId") Long trabajadorId, @Param("estado") EstadoPlanilla estado);
}
