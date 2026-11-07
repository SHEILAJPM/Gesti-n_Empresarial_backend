
package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Trabajador;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    Optional<Trabajador> findByDocumentoIdentidad(String documentoIdentidad);

    @Query("SELECT t FROM Trabajador t WHERE LOWER(t.area) = LOWER(:area)")
    List<Trabajador> buscarPorArea(@Param("area") String area);

    /* SELECT ... FOR UPDATE: serializa las marcaciones de un mismo trabajador */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trabajador t WHERE t.id = :id")
    Optional<Trabajador> bloquear(@Param("id") Long id);
}
