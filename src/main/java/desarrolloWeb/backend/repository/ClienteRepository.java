package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    @Query("""
            SELECT c
            FROM Cliente c
            WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY c.nombre
            """)
    List<Cliente> buscarPorTexto(@Param("texto") String texto);
}
