
package desarrolloWeb.backend.service;

import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.dto.IntentoMarcacionResponse;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.entity.FotoAsistencia;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.repository.FotoAsistenciaRepository;
import desarrolloWeb.backend.repository.IntentoMarcacionRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import desarrolloWeb.backend.dto.ResumenAsistenciaResponse;
import desarrolloWeb.backend.entity.Sede;
import desarrolloWeb.backend.util.CalculadoraAsistencia;
import desarrolloWeb.backend.util.CalculadoraPlanilla;
import desarrolloWeb.backend.repository.MarcacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@Service
public class AsistenciaService {

    private final PersonalService personalService;
    private final MarcacionRepository marcacionRepository;

    private final IntentoMarcacionRepository intentoRepository;
    private final FotoAsistenciaRepository fotoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final SedeService sedeService;
    private final Clock reloj = Clock.system(PortalService.ZONA);

    public AsistenciaService(
            PersonalService personalService,
            MarcacionRepository marcacionRepository,
            IntentoMarcacionRepository intentoRepository,
            FotoAsistenciaRepository fotoRepository,
            TrabajadorRepository trabajadorRepository,
            SedeService sedeService) {
        this.trabajadorRepository = trabajadorRepository;
        this.sedeService = sedeService;
        this.personalService = personalService;
        this.marcacionRepository = marcacionRepository;
        this.intentoRepository = intentoRepository;
        this.fotoRepository = fotoRepository;
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
                        i.getMotivo(), i.getFotoId()))
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

    /* Foto de una marcación o de un intento rechazado, para que RRHH la revise */
    @Transactional(readOnly = true)
    public FotoAsistencia foto(Long fotoId) {
        return fotoRepository.findById(fotoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la foto " + fotoId));
    }

    /* Resumen del mes por trabajador: días, horas, tardanzas y faltas según el horario de la sede */
    @Transactional(readOnly = true)
    public ResumenAsistenciaResponse resumenMensual(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate hoy = LocalDate.now(reloj);
        if (periodo.isAfter(YearMonth.from(hoy))) {
            throw new IllegalArgumentException("No hay asistencia de un mes futuro");
        }
        Sede sede = sedeService.activa().orElse(null);
        LocalTime horaEntrada = sede != null ? sede.getHoraEntrada() : LocalTime.of(8, 0);
        int tolerancia = sede != null ? sede.getToleranciaMinutos() : 10;

        Map<Long, List<Marcacion>> porTrabajador = marcacionRepository
                .findByFechaHoraBetween(periodo.atDay(1).atStartOfDay(), periodo.atEndOfMonth().atTime(LocalTime.MAX))
                .stream().collect(Collectors.groupingBy(Marcacion::getTrabajadorId));

        List<ResumenAsistenciaResponse.Fila> filas = trabajadorRepository.findAll().stream()
                .filter(t -> porTrabajador.containsKey(t.getId())
                        || CalculadoraPlanilla.diasLaborados(t.getFechaIngreso(), t.getFechaCese(), periodo) > 0
                        || (t.getFechaIngreso() == null && t.estaActivo()))
                .map(t -> {
                    CalculadoraAsistencia.Resultado r = CalculadoraAsistencia.calcular(
                            porTrabajador.getOrDefault(t.getId(), List.of()), periodo, t.getFechaIngreso(),
                            t.getFechaCese(), hoy, horaEntrada, tolerancia);
                    return new ResumenAsistenciaResponse.Fila(t.getId(), t.getNombres(), t.getDocumentoIdentidad(),
                            t.getArea(), r.diasLaborables(), r.diasAsistidos(), r.faltas(), r.tardanzas(),
                            r.minutosTardanza(), r.horasTrabajadas(), r.diasSinSalida());
                })
                .sorted(Comparator.comparing(ResumenAsistenciaResponse.Fila::nombres))
                .toList();
        return new ResumenAsistenciaResponse(anio, mes, horaEntrada, tolerancia, filas);
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
