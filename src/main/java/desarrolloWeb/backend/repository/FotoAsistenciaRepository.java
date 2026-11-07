package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.entity.FotoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FotoAsistenciaRepository extends JpaRepository<FotoAsistencia, Long> {

    @Query("SELECT f.id FROM FotoAsistencia f WHERE f.fechaHora < :limite")
    List<Long> idsAnterioresA(@Param("limite") LocalDateTime limite);

    @Modifying
    @Query("UPDATE Marcacion m SET m.fotoId = NULL WHERE m.fotoId IN :ids")
    int desvincularDeMarcaciones(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE IntentoMarcacion i SET i.fotoId = NULL WHERE i.fotoId IN :ids")
    int desvincularDeIntentos(@Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM FotoAsistencia f WHERE f.id IN :ids")
    int borrar(@Param("ids") List<Long> ids);
}
