package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CesarTrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.DocumentoRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PersonalService {

    private final TrabajadorRepository trabajadorRepository;
    private final MarcacionRepository marcacionRepository;
    private final DocumentoRepository documentoRepository;
    private final PagoRepository pagoRepository;

    public PersonalService(TrabajadorRepository trabajadorRepository,
                           MarcacionRepository marcacionRepository,
                           DocumentoRepository documentoRepository,
                           PagoRepository pagoRepository) {
        this.trabajadorRepository = trabajadorRepository;
        this.marcacionRepository = marcacionRepository;
        this.documentoRepository = documentoRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional(readOnly = true)
    public List<TrabajadorResponse> listar(String area, String estado) {
        return trabajadorRepository.findAll().stream()
                .filter(t -> area == null || area.isBlank()
                        || (t.getArea() != null && t.getArea().equalsIgnoreCase(area)))
                .filter(t -> estado == null || estado.isBlank()
                        || (t.getEstado() != null && t.getEstado().equalsIgnoreCase(estado)))
                .map(TrabajadorResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrabajadorResponse> listarPorArea(String area) {
        List<Trabajador> trabajadores = area == null || area.isBlank()
                ? trabajadorRepository.findAll()
                : trabajadorRepository.buscarPorArea(area);
        return trabajadores.stream().map(TrabajadorResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TrabajadorResponse obtener(Long id) {
        return TrabajadorResponse.de(obtenerEntidad(id));
    }

    public Optional<Trabajador> buscarPorId(Long id) {
        return trabajadorRepository.findById(id);
    }

    public Optional<Trabajador> buscarPorDocumento(String documentoIdentidad) {
        return trabajadorRepository.findByDocumentoIdentidad(documentoIdentidad);
    }

    @Transactional
    public TrabajadorResponse registrar(TrabajadorRequest datos) {
        if (trabajadorRepository.existsByDocumentoIdentidad(datos.documentoIdentidad())) {
            throw new ReglaNegocioException(
                    "Ya existe un trabajador registrado con el documento " + datos.documentoIdentidad());
        }
        Trabajador nuevo = new Trabajador();
        nuevo.setDocumentoIdentidad(datos.documentoIdentidad());
        copiarFicha(datos, nuevo);
        nuevo.setEstado(Trabajador.ACTIVO);
        return TrabajadorResponse.de(trabajadorRepository.save(nuevo));
    }

    @Transactional
    public TrabajadorResponse actualizar(Long id, TrabajadorRequest datos) {
        Trabajador existente = obtenerEntidad(id);
        if (existente.getFechaCese() != null && datos.fechaIngreso().isAfter(existente.getFechaCese())) {
            throw new ReglaNegocioException("La fecha de ingreso no puede ser posterior a la fecha de cese");
        }
        // El documento de identidad no se modifica
        copiarFicha(datos, existente);
        return TrabajadorResponse.de(trabajadorRepository.save(existente));
    }

    /* Dar de baja: el trabajador deja de poder marcar asistencia, pero su historial se conserva */
    @Transactional
    public TrabajadorResponse cesar(Long id, CesarTrabajadorRequest datos) {
        Trabajador trabajador = obtenerEntidad(id);
        if (!trabajador.estaActivo()) {
            throw new ReglaNegocioException(trabajador.getNombres() + " ya está cesado");
        }
        if (trabajador.getFechaIngreso() != null && datos.fechaCese().isBefore(trabajador.getFechaIngreso())) {
            throw new ReglaNegocioException("La fecha de cese no puede ser anterior a la fecha de ingreso ("
                    + trabajador.getFechaIngreso() + ")");
        }
        trabajador.setEstado(Trabajador.CESADO);
        trabajador.setFechaCese(datos.fechaCese());
        trabajador.setMotivoCese(datos.motivo().trim());
        return TrabajadorResponse.de(trabajadorRepository.save(trabajador));
    }

    @Transactional
    public TrabajadorResponse reactivar(Long id) {
        Trabajador trabajador = obtenerEntidad(id);
        if (trabajador.estaActivo()) {
            throw new ReglaNegocioException(trabajador.getNombres() + " ya está activo");
        }
        trabajador.setEstado(Trabajador.ACTIVO);
        trabajador.setFechaCese(null);
        trabajador.setMotivoCese(null);
        return TrabajadorResponse.de(trabajadorRepository.save(trabajador));
    }

    /* Solo para registros hechos por error: con historial hay que cesarlo */
    @Transactional
    public void eliminar(Long id) {
        Trabajador trabajador = obtenerEntidad(id);
        if (marcacionRepository.existsByTrabajadorId(id)
                || documentoRepository.existsByTrabajadorId(id)
                || pagoRepository.existsByTrabajador_Id(id)) {
            throw new ReglaNegocioException(trabajador.getNombres()
                    + " tiene asistencias, documentos o pagos registrados; dalo de baja en lugar de eliminarlo");
        }
        trabajadorRepository.delete(trabajador);
    }

    private Trabajador obtenerEntidad(Long id) {
        return trabajadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un trabajador con id " + id));
    }

    private static void copiarFicha(TrabajadorRequest datos, Trabajador destino) {
        destino.setNombres(datos.nombres().trim());
        destino.setCargo(datos.cargo().trim());
        destino.setArea(datos.area().trim());
        destino.setFechaIngreso(datos.fechaIngreso());
        destino.setSueldo(datos.sueldo());
        destino.setFechaNacimiento(datos.fechaNacimiento());
        destino.setTelefono(vacioANulo(datos.telefono()));
        destino.setCorreo(vacioANulo(datos.correo()));
        destino.setDireccion(vacioANulo(datos.direccion()));
        String sistema = vacioANulo(datos.sistemaPension());
        destino.setSistemaPension(sistema);
        // El nombre de la AFP solo aplica si el sistema es AFP
        destino.setAfp("AFP".equals(sistema) ? vacioANulo(datos.afp()) : null);
        destino.setBanco(vacioANulo(datos.banco()));
        destino.setNumeroCuenta(vacioANulo(datos.numeroCuenta()));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
