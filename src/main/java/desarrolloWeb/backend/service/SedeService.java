package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.SedeRequest;
import desarrolloWeb.backend.dto.SedeResponse;
import desarrolloWeb.backend.entity.Sede;
import desarrolloWeb.backend.repository.SedeRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/* La sede activa es el punto desde el que se mide la distancia al marcar asistencia */
@Service
public class SedeService {

    private final SedeRepository sedeRepository;

    public SedeService(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Sede> activa() {
        return sedeRepository.findFirstByActivaTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<SedeResponse> obtener() {
        return activa().map(SedeResponse::de);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SedeResponse guardar(SedeRequest datos) {
        Sede sede = activa().orElseGet(Sede::new);
        sede.setNombre(datos.nombre().trim());
        sede.setDireccion(datos.direccion() == null || datos.direccion().isBlank() ? null : datos.direccion().trim());
        sede.setLatitud(datos.latitud());
        sede.setLongitud(datos.longitud());
        sede.setRadioMetros(datos.radioMetros());
        sede.setPrecisionMaxima(datos.precisionMaxima());
        sede.setActiva(true);
        return SedeResponse.de(sedeRepository.save(sede));
    }
}
