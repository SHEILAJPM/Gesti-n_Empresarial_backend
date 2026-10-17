package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    Optional<Sede> findFirstByActivaTrueOrderByIdAsc();
}
