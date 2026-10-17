package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.ConfiguracionTributaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionTributariaRepository extends JpaRepository<ConfiguracionTributaria, Long> {

    Optional<ConfiguracionTributaria> findByCodigo(String codigo);

    @Query("""
            SELECT c
            FROM ConfiguracionTributaria c
            WHERE c.codigo = :codigo
              AND c.vigente = true
            """)
    Optional<ConfiguracionTributaria> buscarVigente(@Param("codigo") String codigo);

    @Query("""
            SELECT c
            FROM ConfiguracionTributaria c
            ORDER BY c.codigo
            """)
    List<ConfiguracionTributaria> listarOrdenadas();
}
