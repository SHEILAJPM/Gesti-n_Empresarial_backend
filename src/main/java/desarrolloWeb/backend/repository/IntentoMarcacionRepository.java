package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.IntentoMarcacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoMarcacionRepository extends JpaRepository<IntentoMarcacion, Long> {

    List<IntentoMarcacion> findTop200ByOrderByFechaHoraDesc();
}
