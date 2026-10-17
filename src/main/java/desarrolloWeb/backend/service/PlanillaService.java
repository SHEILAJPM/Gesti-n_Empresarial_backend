package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.PlanillaResponse;
import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.Planilla;
import desarrolloWeb.backend.entity.PlanillaDetalle;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.PlanillaRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import desarrolloWeb.backend.util.CalculadoraPlanilla;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/* RRHH genera la planilla del mes (BORRADOR, se puede regenerar);
   Contabilidad la aprueba y se crea un pago pendiente por trabajador en el módulo de Pagos. */
@Service
public class PlanillaService {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");

    private final PlanillaRepository planillaRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PagoRepository pagoRepository;
    private final PagoService pagoService;

    public PlanillaService(PlanillaRepository planillaRepository, TrabajadorRepository trabajadorRepository,
                           PagoRepository pagoRepository, PagoService pagoService) {
        this.planillaRepository = planillaRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.pagoRepository = pagoRepository;
        this.pagoService = pagoService;
    }

    @Transactional(readOnly = true)
    public List<PlanillaResponse> listar() {
        return planillaRepository.findAllByOrderByAnioDescMesDesc().stream()
                .map(p -> toResponse(p, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanillaResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id), true);
    }

    @Transactional
    public PlanillaResponse generar(int anio, int mes, String usuario) {
        YearMonth periodo = YearMonth.of(anio, mes);
        if (periodo.isAfter(YearMonth.now())) {
            throw new ReglaNegocioException("No se puede generar la planilla de un mes futuro");
        }

        planillaRepository.findByAnioAndMes(anio, mes).ifPresent(existente -> {
            if (existente.getEstado() == EstadoPlanilla.APROBADA) {
                throw new ReglaNegocioException("La planilla de " + nombrePeriodo(periodo)
                        + " ya fue aprobada y no se puede regenerar");
            }
            // Un borrador se reemplaza por el nuevo cálculo
            planillaRepository.delete(existente);
            planillaRepository.flush();
        });

        Planilla planilla = new Planilla();
        planilla.setAnio(anio);
        planilla.setMes(mes);
        planilla.setEstado(EstadoPlanilla.BORRADOR);
        planilla.setFechaGeneracion(LocalDateTime.now());
        planilla.setGeneradaPor(usuario);

        for (Trabajador t : trabajadorRepository.findAll()) {
            int dias = CalculadoraPlanilla.diasLaborados(t.getFechaIngreso(), t.getFechaCese(), periodo);
            if (dias > 0 && motivoOmision(t) == null) {
                planilla.agregar(crearDetalle(t, dias));
            }
        }
        if (planilla.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("No hay trabajadores con la ficha completa (ingreso, sueldo y sistema de "
                    + "pensiones) en " + nombrePeriodo(periodo));
        }
        planilla.totalizar();
        return toResponse(planillaRepository.save(planilla), true);
    }

    @Transactional
    public PlanillaResponse aprobar(Long id, LocalDate fechaPago, String usuario) {
        Planilla planilla = obtenerParaActualizar(id);
        if (planilla.getEstado() != EstadoPlanilla.BORRADOR) {
            throw new ReglaNegocioException("La planilla ya está aprobada");
        }
        String concepto = "Sueldo " + nombrePeriodo(YearMonth.of(planilla.getAnio(), planilla.getMes()));
        for (PlanillaDetalle d : planilla.getDetalles()) {
            Pago pago = pagoService.registrar(d.getDocumentoIdentidad(), concepto, d.getNeto(), fechaPago);
            d.setPagoId(pago.getId());
        }
        planilla.setEstado(EstadoPlanilla.APROBADA);
        planilla.setFechaAprobacion(LocalDateTime.now());
        planilla.setAprobadaPor(usuario);
        planilla.setFechaPago(fechaPago);
        return toResponse(planillaRepository.save(planilla), true);
    }

    @Transactional
    public void eliminar(Long id) {
        Planilla planilla = obtenerParaActualizar(id);
        if (planilla.getEstado() == EstadoPlanilla.APROBADA) {
            throw new ReglaNegocioException("Una planilla aprobada no se puede eliminar: ya generó los pagos");
        }
        planillaRepository.delete(planilla);
    }

    private Planilla obtenerEntidad(Long id) {
        return planillaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planilla " + id));
    }

    /* Bloquea la fila hasta terminar la transacción: aprobar o eliminar no pueden pisarse entre sí */
    private Planilla obtenerParaActualizar(Long id) {
        return planillaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la planilla " + id));
    }

    /* null si el trabajador puede entrar en planilla; si no, el dato que le falta */
    private static String motivoOmision(Trabajador t) {
        if (t.getSueldo() == null) {
            return "sin sueldo";
        }
        if (t.getSistemaPension() == null) {
            return "sin sistema de pensiones";
        }
        if ("AFP".equals(t.getSistemaPension()) && !CalculadoraPlanilla.COMISION_AFP.containsKey(t.getAfp())) {
            return "sin AFP registrada";
        }
        return null;
    }

    private static PlanillaDetalle crearDetalle(Trabajador t, int dias) {
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(t.getSueldo(), dias, t.getSistemaPension(), t.getAfp());
        PlanillaDetalle d = new PlanillaDetalle();
        d.setTrabajadorId(t.getId());
        d.setNombres(t.getNombres());
        d.setDocumentoIdentidad(t.getDocumentoIdentidad());
        d.setCargo(t.getCargo());
        d.setArea(t.getArea());
        d.setFechaIngreso(t.getFechaIngreso());
        d.setSueldoBase(t.getSueldo());
        d.setDiasLaborados(c.diasLaborados());
        d.setRemuneracionBruta(c.remuneracionBruta());
        d.setSistemaPension(t.getSistemaPension());
        d.setAfp(t.getAfp());
        d.setTasaPension(c.tasaPension());
        d.setDescuentoPension(c.descuentoPension());
        d.setEssalud(c.essalud());
        d.setNeto(c.neto());
        return d;
    }

    private PlanillaResponse toResponse(Planilla p, boolean completo) {
        List<Long> idsPago = p.getDetalles().stream().map(PlanillaDetalle::getPagoId).filter(Objects::nonNull).toList();
        Map<Long, String> estadoPagos = idsPago.isEmpty() ? Map.of()
                : pagoRepository.findAllById(idsPago).stream().collect(Collectors.toMap(Pago::getId, Pago::getEstado));
        int pagados = (int) estadoPagos.values().stream().filter("PAGADO"::equals).count();

        List<PlanillaResponse.Detalle> detalles = !completo ? List.of() : p.getDetalles().stream()
                .map(d -> PlanillaResponse.Detalle.de(d, d.getPagoId() == null ? null : estadoPagos.get(d.getPagoId())))
                .toList();

        return new PlanillaResponse(p.getId(), p.getAnio(), p.getMes(), p.getEstado(), p.getFechaGeneracion(),
                p.getGeneradaPor(), p.getFechaAprobacion(), p.getAprobadaPor(), p.getFechaPago(), p.getTotalBruto(),
                p.getTotalDescuentos(), p.getTotalNeto(), p.getTotalEssalud(), p.getDetalles().size(), pagados,
                detalles, completo && p.getEstado() == EstadoPlanilla.BORRADOR ? omitidos(p) : List.of());
    }

    /* Trabajadores que estuvieron en el periodo pero quedaron fuera por ficha incompleta */
    private List<String> omitidos(Planilla p) {
        YearMonth periodo = YearMonth.of(p.getAnio(), p.getMes());
        Map<Long, PlanillaDetalle> incluidos = p.getDetalles().stream()
                .collect(Collectors.toMap(PlanillaDetalle::getTrabajadorId, Function.identity()));
        List<String> lista = new ArrayList<>();
        for (Trabajador t : trabajadorRepository.findAll()) {
            if (incluidos.containsKey(t.getId())) {
                continue;
            }
            if (t.getFechaIngreso() == null && t.estaActivo()) {
                lista.add(t.getNombres() + ": sin fecha de ingreso");
            } else if (CalculadoraPlanilla.diasLaborados(t.getFechaIngreso(), t.getFechaCese(), periodo) > 0) {
                String motivo = motivoOmision(t);
                if (motivo != null) {
                    lista.add(t.getNombres() + ": " + motivo);
                }
            }
        }
        return lista;
    }

    static String nombrePeriodo(YearMonth periodo) {
        return periodo.getMonth().getDisplayName(TextStyle.FULL, ES_PE) + " " + periodo.getYear();
    }
}
