package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.FotoAsistencia;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.FotoAsistenciaRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("Retención de fotos de asistencia")
class LimpiezaFotosServiceTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 6, 3, 30);

    @Autowired private FotoAsistenciaRepository fotoRepository;
    @Autowired private MarcacionRepository marcacionRepository;
    @Autowired private TrabajadorRepository trabajadorRepository;
    @Autowired private EntityManager em;

    @Test
    void borraLasFotosVencidasYConservaLaMarcacion() {
        Trabajador t = trabajadorRepository.save(new Trabajador(null, "Rosa Díaz", "70000007", "Vendedora", "Ventas", Trabajador.ACTIVO));
        Long vieja = fotoRepository.save(new FotoAsistencia(t.getId(), HOY.minusDays(200), "image/jpeg", new byte[]{1})).getId();
        Long nueva = fotoRepository.save(new FotoAsistencia(t.getId(), HOY.minusDays(10), "image/jpeg", new byte[]{2})).getId();
        Marcacion m = new Marcacion();
        m.setTrabajadorId(t.getId());
        m.setTipo(TipoMarcacion.ENTRADA);
        m.setFechaHora(HOY.minusDays(200));
        m.setOrigen("PORTAL");
        m.setFotoId(vieja);
        Long marcacionId = marcacionRepository.save(m).getId();
        em.flush();

        Clock reloj = Clock.fixed(ZonedDateTime.of(HOY, PortalService.ZONA).toInstant(), PortalService.ZONA);
        int borradas = new LimpiezaFotosService(fotoRepository, 180, reloj).borrarAntiguas();
        em.flush();
        em.clear();

        assertEquals(1, borradas);
        assertFalse(fotoRepository.existsById(vieja));
        assertTrue(fotoRepository.existsById(nueva));
        assertNull(marcacionRepository.findById(marcacionId).orElseThrow().getFotoId());
    }
}
