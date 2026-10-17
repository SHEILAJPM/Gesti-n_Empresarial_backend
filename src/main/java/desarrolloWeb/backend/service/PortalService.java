package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.MarcarUbicacionRequest;
import desarrolloWeb.backend.dto.MiBoletaResponse;
import desarrolloWeb.backend.dto.MiPerfilResponse;
import desarrolloWeb.backend.dto.PlanillaResponse;
import desarrolloWeb.backend.dto.SedeResponse;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.IntentoMarcacion;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.Sede;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.entity.Usuario;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.IntentoMarcacionRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.PlanillaDetalleRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import desarrolloWeb.backend.repository.UsuarioRepository;
import desarrolloWeb.backend.util.Distancia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* "Mi espacio": lo que hace el propio trabajador con su cuenta (marcar asistencia por GPS, ver sus boletas).
   La identidad sale del usuario autenticado, nunca de un DNI enviado por el cliente. */
@Service
public class PortalService {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final UsuarioRepository usuarioRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final MarcacionRepository marcacionRepository;
    private final IntentoMarcacionRepository intentoRepository;
    private final PlanillaDetalleRepository detalleRepository;
    private final PagoRepository pagoRepository;
    private final SedeService sedeService;
    private final Clock reloj;

    @Autowired
    public PortalService(UsuarioRepository usuarioRepository, TrabajadorRepository trabajadorRepository,
                         MarcacionRepository marcacionRepository, IntentoMarcacionRepository intentoRepository,
                         PlanillaDetalleRepository detalleRepository, PagoRepository pagoRepository,
                         SedeService sedeService) {
        this(usuarioRepository, trabajadorRepository, marcacionRepository, intentoRepository, detalleRepository,
                pagoRepository, sedeService, Clock.system(ZONA));
    }

    PortalService(UsuarioRepository usuarioRepository, TrabajadorRepository trabajadorRepository,
                  MarcacionRepository marcacionRepository, IntentoMarcacionRepository intentoRepository,
                  PlanillaDetalleRepository detalleRepository, PagoRepository pagoRepository,
                  SedeService sedeService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.marcacionRepository = marcacionRepository;
        this.intentoRepository = intentoRepository;
        this.detalleRepository = detalleRepository;
        this.pagoRepository = pagoRepository;
        this.sedeService = sedeService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public MiPerfilResponse perfil(String username) {
        Usuario usuario = usuario(username);
        Trabajador trabajador = trabajadorDe(usuario);
        List<Marcacion> hoy = marcacionesDelDia(trabajador.getId(), LocalDate.now(reloj));
        return new MiPerfilResponse(usuario.getUsername(), usuario.getRol(), TrabajadorResponse.de(trabajador),
                sedeService.activa().map(SedeResponse::de).orElse(null), siguienteTipo(hoy), hoy);
    }

    /* Sin @Transactional a propósito: el intento rechazado se guarda aunque luego se lance la excepción */
    public Marcacion marcar(String username, MarcarUbicacionRequest ubicacion) {
        Trabajador trabajador = trabajadorDe(usuario(username));
        if (!trabajador.estaActivo()) {
            throw new ReglaNegocioException("Tu ficha está dada de baja; no puedes registrar asistencia");
        }
        Sede sede = sedeService.activa().orElseThrow(() -> new ReglaNegocioException(
                "La empresa todavía no configuró su ubicación. Avisa al administrador."));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        TipoMarcacion tipo = siguienteTipo(marcacionesDelDia(trabajador.getId(), ahora.toLocalDate()));
        double distancia = Distancia.metros(sede.getLatitud().doubleValue(), sede.getLongitud().doubleValue(),
                ubicacion.latitud(), ubicacion.longitud());

        if (ubicacion.precision() > sede.getPrecisionMaxima()) {
            rechazar(trabajador, tipo, ahora, ubicacion, distancia, String.format(
                    "Tu GPS tiene un margen de ±%.0f m (máximo ±%d m). Sal a un lugar despejado o activa la ubicación precisa.",
                    ubicacion.precision(), sede.getPrecisionMaxima()));
        }
        if (distancia > sede.getRadioMetros()) {
            rechazar(trabajador, tipo, ahora, ubicacion, distancia, String.format(
                    "Estás a %.0f m de %s; debes estar a máximo %d m para marcar.",
                    distancia, sede.getNombre(), sede.getRadioMetros()));
        }

        Marcacion marcacion = new Marcacion();
        marcacion.setTrabajadorId(trabajador.getId());
        marcacion.setTipo(tipo);
        marcacion.setFechaHora(ahora);
        marcacion.setOrigen("PORTAL");
        marcacion.setLatitud(decimal(ubicacion.latitud(), 7));
        marcacion.setLongitud(decimal(ubicacion.longitud(), 7));
        marcacion.setPrecisionMetros(decimal(ubicacion.precision(), 2));
        marcacion.setDistanciaMetros(decimal(distancia, 2));
        return marcacionRepository.save(marcacion);
    }

    @Transactional(readOnly = true)
    public List<Marcacion> misMarcaciones(String username) {
        Trabajador trabajador = trabajadorDe(usuario(username));
        return marcacionRepository.findByTrabajadorId(trabajador.getId()).stream()
                .sorted(Comparator.comparing(Marcacion::getFechaHora).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MiBoletaResponse> misBoletas(String username) {
        Trabajador trabajador = trabajadorDe(usuario(username));
        var detalles = detalleRepository.buscarBoletas(trabajador.getId(), EstadoPlanilla.APROBADA);
        List<Long> idsPago = detalles.stream().map(d -> d.getPagoId()).filter(id -> id != null).toList();
        Map<Long, String> estados = idsPago.isEmpty() ? Map.of()
                : pagoRepository.findAllById(idsPago).stream().collect(Collectors.toMap(Pago::getId, Pago::getEstado));
        return detalles.stream()
                .map(d -> new MiBoletaResponse(d.getPlanilla().getAnio(), d.getPlanilla().getMes(),
                        d.getPlanilla().getFechaPago(), PlanillaResponse.Detalle.de(d, d.getPagoId() == null ? null : estados.get(d.getPagoId()))))
                .toList();
    }

    private void rechazar(Trabajador t, TipoMarcacion tipo, LocalDateTime ahora, MarcarUbicacionRequest u,
                          double distancia, String motivo) {
        IntentoMarcacion intento = new IntentoMarcacion();
        intento.setTrabajadorId(t.getId());
        intento.setTipo(tipo);
        intento.setFechaHora(ahora);
        intento.setLatitud(decimal(u.latitud(), 7));
        intento.setLongitud(decimal(u.longitud(), 7));
        intento.setPrecisionMetros(decimal(Math.min(u.precision(), 999_999), 2));
        intento.setDistanciaMetros(decimal(Math.min(distancia, 999_999), 2));
        intento.setMotivo(motivo.length() > 200 ? motivo.substring(0, 200) : motivo);
        intentoRepository.save(intento);
        throw new ReglaNegocioException(motivo);
    }

    private List<Marcacion> marcacionesDelDia(Long trabajadorId, LocalDate dia) {
        return marcacionRepository.findByTrabajadorId(trabajadorId).stream()
                .filter(m -> m.getFechaHora().toLocalDate().equals(dia))
                .sorted(Comparator.comparing(Marcacion::getFechaHora))
                .toList();
    }

    /* Entrada y salida se alternan durante el día; así no se puede marcar dos entradas seguidas */
    static TipoMarcacion siguienteTipo(List<Marcacion> delDiaOrdenadas) {
        return delDiaOrdenadas.isEmpty() || delDiaOrdenadas.get(delDiaOrdenadas.size() - 1).getTipo() == TipoMarcacion.SALIDA
                ? TipoMarcacion.ENTRADA
                : TipoMarcacion.SALIDA;
    }

    private Usuario usuario(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
    }

    private Trabajador trabajadorDe(Usuario usuario) {
        if (usuario.getTrabajadorId() == null) {
            throw new ReglaNegocioException("Tu cuenta no está vinculada a una ficha de trabajador. Pídele a RRHH o al administrador que la vincule.");
        }
        return trabajadorRepository.findById(usuario.getTrabajadorId())
                .orElseThrow(() -> new ReglaNegocioException("La ficha vinculada a tu cuenta ya no existe"));
    }

    private static BigDecimal decimal(double valor, int escala) {
        return BigDecimal.valueOf(valor).setScale(escala, RoundingMode.HALF_UP);
    }
}
