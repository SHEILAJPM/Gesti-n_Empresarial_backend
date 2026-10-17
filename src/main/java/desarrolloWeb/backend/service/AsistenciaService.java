
package desarrolloWeb.backend.service;

import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.dto.IntentoMarcacionResponse;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.repository.IntentoMarcacionRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@Service
public class AsistenciaService {

    private final PersonalService personalService;
    private final MarcacionRepository marcacionRepository;

    private final IntentoMarcacionRepository intentoRepository;

    public AsistenciaService(
            PersonalService personalService,
            MarcacionRepository marcacionRepository,
            IntentoMarcacionRepository intentoRepository) {
        this.personalService = personalService;
        this.marcacionRepository = marcacionRepository;
        this.intentoRepository = intentoRepository;
    }

    /* Marcaciones rechazadas en el portal (fuera del radio o GPS impreciso), las más recientes primero */
    @Transactional(readOnly = true)
    public List<IntentoMarcacionResponse> listarIntentos() {
        Map<Long, String> nombres = personalService.listar(null, null).stream()
                .collect(Collectors.toMap(TrabajadorResponse::id, TrabajadorResponse::nombres));
        return intentoRepository.findTop200ByOrderByFechaHoraDesc().stream()
                .map(i -> new IntentoMarcacionResponse(i.getId(), i.getTrabajadorId(),
                        nombres.getOrDefault(i.getTrabajadorId(), "Trabajador #" + i.getTrabajadorId()), i.getFechaHora(),
                        i.getTipo(), i.getDistanciaMetros(), i.getPrecisionMetros(), i.getLatitud(), i.getLongitud(),
                        i.getMotivo()))
                .toList();
    }

    @Transactional
    public Marcacion marcar(
            String documentoIdentidad,
            TipoMarcacion tipo,
            LocalDateTime fechaHora) {

        Trabajador trabajador =
                personalService.buscarPorDocumento(documentoIdentidad)
                        .orElseThrow(() -> new NoSuchElementException(
                                "No existe un trabajador con documento "
                                        + documentoIdentidad));

        if (!trabajador.estaActivo()) {
            throw new ReglaNegocioException(trabajador.getNombres()
                    + " está cesado y no puede registrar asistencia");
        }

        Marcacion marcacion = new Marcacion();
        marcacion.setTrabajadorId(trabajador.getId());
        marcacion.setTipo(tipo);
        // La hora la pone el servidor: el registro manual de RRHH tampoco acepta horas enviadas por el cliente
        marcacion.setFechaHora(LocalDateTime.now(PortalService.ZONA));
        marcacion.setOrigen("RRHH");

        return marcacionRepository.save(marcacion);
    }

    public List<Marcacion> listarTodas() {
        return marcacionRepository.findAll();
    }

    public List<Marcacion> listarPorTrabajador(Long trabajadorId) {
        return marcacionRepository.findByTrabajadorId(trabajadorId);
    }
    
public List<Marcacion> listarPorTrabajadorJPQL(Long trabajadorId) {
    return marcacionRepository.buscarPorTrabajadorJPQL(trabajadorId);
}

}
