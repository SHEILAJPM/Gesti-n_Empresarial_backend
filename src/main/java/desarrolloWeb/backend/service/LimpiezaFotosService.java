package desarrolloWeb.backend.service;

import desarrolloWeb.backend.repository.FotoAsistenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/* Las fotos ocupan la mayor parte de la base. Pasado el plazo de retención se borran: la marcación se conserva
   (hora, ubicación, distancia), solo pierde la imagen. Corre todas las madrugadas. */
@Service
public class LimpiezaFotosService {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaFotosService.class);
    /* Se borra por tandas para no armar una sentencia IN gigante */
    private static final int TANDA = 500;

    private final FotoAsistenciaRepository fotoRepository;
    private final int diasRetencion;
    private final Clock reloj;

    @Autowired
    public LimpiezaFotosService(FotoAsistenciaRepository fotoRepository,
                                @Value("${asistencia.fotos.retencion-dias:180}") int diasRetencion) {
        this(fotoRepository, diasRetencion, Clock.system(PortalService.ZONA));
    }

    LimpiezaFotosService(FotoAsistenciaRepository fotoRepository, int diasRetencion, Clock reloj) {
        this.fotoRepository = fotoRepository;
        this.diasRetencion = diasRetencion;
        this.reloj = reloj;
    }

    @Scheduled(cron = "${asistencia.fotos.limpieza-cron:0 30 3 * * *}", zone = "America/Lima")
    @Transactional
    public int borrarAntiguas() {
        if (diasRetencion <= 0) {
            return 0;
        }
        LocalDateTime limite = LocalDateTime.now(reloj).minusDays(diasRetencion);
        List<Long> ids = fotoRepository.idsAnterioresA(limite);
        for (int i = 0; i < ids.size(); i += TANDA) {
            List<Long> tanda = ids.subList(i, Math.min(ids.size(), i + TANDA));
            fotoRepository.desvincularDeMarcaciones(tanda);
            fotoRepository.desvincularDeIntentos(tanda);
            fotoRepository.borrar(tanda);
        }
        if (!ids.isEmpty()) {
            log.info("Se borraron {} fotos de asistencia anteriores a {}", ids.size(), limite.toLocalDate());
        }
        return ids.size();
    }
}
