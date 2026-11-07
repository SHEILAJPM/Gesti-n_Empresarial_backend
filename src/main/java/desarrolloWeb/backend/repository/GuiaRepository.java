package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.Guia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuiaRepository extends JpaRepository<Guia, Long> {

    @Query("SELECT g FROM Guia g LEFT JOIN FETCH g.venta ORDER BY g.fechaEmision DESC, g.id DESC")
    List<Guia> listarConVenta();

    List<Guia> findByVentaIdOrderByIdAsc(Long ventaId);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(numero, :desde) AS BIGINT)) FROM guias "
            + "WHERE numero LIKE CONCAT(:prefijo, '%')", nativeQuery = true)
    Long maximoCorrelativo(@Param("prefijo") String prefijo, @Param("desde") int desde);
}
