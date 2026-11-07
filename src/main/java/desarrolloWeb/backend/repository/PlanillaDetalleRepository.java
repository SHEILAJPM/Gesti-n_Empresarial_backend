package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.PlanillaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Long> {

    boolean existsByTrabajadorId(Long trabajadorId);

    /* Boletas del trabajador en planillas aprobadas, de la más reciente a la más antigua */
    @Query("SELECT d FROM PlanillaDetalle d JOIN FETCH d.planilla p WHERE d.trabajadorId = :trabajadorId "
            + "AND p.estado = :estado ORDER BY p.anio DESC, p.mes DESC")
    List<PlanillaDetalle> buscarBoletas(@Param("trabajadorId") Long trabajadorId, @Param("estado") EstadoPlanilla estado);

    /* Lo ya pagado al trabajador en meses anteriores del mismo año (para proyectar la renta de 5ta) */
    @Query("SELECT d FROM PlanillaDetalle d JOIN d.planilla p WHERE d.trabajadorId = :trabajadorId "
            + "AND p.anio = :anio AND p.mes < :mes AND p.estado = :estado")
    List<PlanillaDetalle> buscarAnterioresDelAnio(@Param("trabajadorId") Long trabajadorId, @Param("anio") Integer anio,
                                                  @Param("mes") Integer mes, @Param("estado") EstadoPlanilla estado);
}
