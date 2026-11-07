package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CobranzaDtos.CobroRequest;
import desarrolloWeb.backend.dto.CobranzaDtos.CobroResponse;
import desarrolloWeb.backend.dto.CobranzaDtos.CuentaPorCobrar;
import desarrolloWeb.backend.dto.CobranzaDtos.DeudaCliente;
import desarrolloWeb.backend.dto.CobranzaDtos.EstadoCuenta;
import desarrolloWeb.backend.dto.CobranzaDtos.EstadoDeCuenta;
import desarrolloWeb.backend.dto.CobranzaDtos.Resumen;
import desarrolloWeb.backend.dto.CobranzaDtos.Tramo;
import desarrolloWeb.backend.entity.Cobro;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.FormaPago;
import desarrolloWeb.backend.entity.Nota;
import desarrolloWeb.backend.entity.TipoNota;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.CobroRepository;
import desarrolloWeb.backend.repository.NotaRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.CalculadoraCobranza;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Cuentas por cobrar: lo que deben los clientes por las ventas al crédito y los cobros que se registran.
   Deuda = cuotas pactadas + notas de débito − notas de crédito. Saldo = deuda − cobros. */
@Service
public class CobranzaService {

    private final VentaRepository ventaRepository;
    private final CobroRepository cobroRepository;
    private final NotaRepository notaRepository;
    private final Clock reloj;

    @Autowired
    public CobranzaService(VentaRepository ventaRepository, CobroRepository cobroRepository, NotaRepository notaRepository) {
        this(ventaRepository, cobroRepository, notaRepository, Clock.system(PortalService.ZONA));
    }

    CobranzaService(VentaRepository ventaRepository, CobroRepository cobroRepository, NotaRepository notaRepository,
                    Clock reloj) {
        this.ventaRepository = ventaRepository;
        this.cobroRepository = cobroRepository;
        this.notaRepository = notaRepository;
        this.reloj = reloj;
    }

    /* Facturas al crédito con saldo (o todas, con incluirPagadas) */
    @Transactional(readOnly = true)
    public List<CuentaPorCobrar> listar(boolean incluirPagadas) {
        Map<Long, BigDecimal> cobrado = new HashMap<>();
        for (Object[] fila : cobroRepository.totalesPorVenta()) {
            cobrado.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        Map<Long, BigDecimal> creditos = new HashMap<>();
        for (Object[] fila : notaRepository.totalesPorVentaYTipo()) {
            if (fila[1] == TipoNota.CREDITO) {
                creditos.put((Long) fila[0], (BigDecimal) fila[2]);
            }
        }
        Map<Long, List<Nota>> debitos = new HashMap<>();
        for (Nota n : notaRepository.findByTipo(TipoNota.DEBITO)) {
            debitos.computeIfAbsent(n.getVenta().getId(), k -> new ArrayList<>()).add(n);
        }
        LocalDate hoy = LocalDate.now(reloj);
        List<CuentaPorCobrar> lista = new ArrayList<>();
        for (Venta v : ventaRepository.listarAlCredito()) {
            CalculadoraCobranza.Resultado r = calcular(v, creditos.getOrDefault(v.getId(), BigDecimal.ZERO),
                    debitos.getOrDefault(v.getId(), List.of()), cobrado.getOrDefault(v.getId(), BigDecimal.ZERO), hoy);
            CuentaPorCobrar cuenta = cuenta(v, r);
            if (incluirPagadas || cuenta.estado() != EstadoCuenta.PAGADA) {
                lista.add(cuenta);
            }
        }
        lista.sort(Comparator.comparing(CuentaPorCobrar::estado).thenComparing(CuentaPorCobrar::diasAtraso, Comparator.reverseOrder()));
        return lista;
    }

    @Transactional(readOnly = true)
    public EstadoDeCuenta estadoDeCuenta(Long ventaId) {
        Venta v = ventaAlCredito(ventaId);
        List<Nota> notas = notaRepository.findByVentaIdOrderByIdAsc(ventaId);
        BigDecimal nc = notas.stream().filter(n -> n.getTipo() == TipoNota.CREDITO).map(Nota::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Nota> nd = notas.stream().filter(n -> n.getTipo() == TipoNota.DEBITO).toList();
        List<Cobro> cobros = cobroRepository.findByVentaIdOrderByFechaAscIdAsc(ventaId);
        BigDecimal cobrado = cobros.stream().map(Cobro::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        CalculadoraCobranza.Resultado r = calcular(v, nc, nd, cobrado, LocalDate.now(reloj));
        return new EstadoDeCuenta(cuenta(v, r), nc,
                nd.stream().map(Nota::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                r.tramos().stream().map(t -> new Tramo(t.descripcion(), t.vence(), t.monto(), t.cobrado(), t.pendiente(),
                        t.estado(), t.diasVencido())).toList(),
                cobros.stream().map(CobranzaService::respuesta).toList());
    }

    @Transactional
    public EstadoDeCuenta registrarCobro(CobroRequest datos, String usuario) {
        Venta v = ventaAlCredito(datos.ventaId());
        LocalDate fecha = datos.fecha() != null ? datos.fecha() : LocalDate.now(reloj);
        if (fecha.isBefore(v.getFecha())) {
            throw new IllegalArgumentException("El cobro no puede ser anterior a la venta (" + v.getFecha() + ")");
        }
        BigDecimal saldo = estadoDeCuenta(v.getId()).cuenta().saldo();
        BigDecimal monto = datos.monto().setScale(2, RoundingMode.HALF_UP);
        if (saldo.signum() == 0) {
            throw new ReglaNegocioException("La venta " + v.getNumeroComprobante() + " ya está cobrada por completo");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El cobro (S/ " + monto + ") supera el saldo pendiente (S/ " + saldo + ")");
        }
        Cobro c = new Cobro();
        c.setVenta(v);
        c.setFecha(fecha);
        c.setMonto(monto);
        c.setMedio(datos.medio());
        c.setReferencia(datos.referencia() == null || datos.referencia().isBlank() ? null : datos.referencia().trim());
        c.setRegistradoPor(usuario);
        c.setRegistradoEn(LocalDateTime.now(reloj));
        cobroRepository.save(c);
        return estadoDeCuenta(v.getId());
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        List<CuentaPorCobrar> pendientes = listar(false);
        Map<Long, DeudaCliente> porCliente = new LinkedHashMap<>();
        for (CuentaPorCobrar c : pendientes) {
            porCliente.merge(c.clienteId(),
                    new DeudaCliente(c.clienteId(), c.documentoCliente(), c.nombreCliente(), 1, c.saldo(), c.vencido()),
                    (a, b) -> new DeudaCliente(a.clienteId(), a.documento(), a.nombre(), a.facturas() + 1,
                            a.saldo().add(b.saldo()), a.vencido().add(b.vencido())));
        }
        YearMonth mes = YearMonth.now(reloj);
        return new Resumen(
                pendientes.stream().map(CuentaPorCobrar::saldo).reduce(BigDecimal.ZERO, BigDecimal::add),
                pendientes.stream().map(CuentaPorCobrar::vencido).reduce(BigDecimal.ZERO, BigDecimal::add),
                cobroRepository.totalEntre(mes.atDay(1), mes.atEndOfMonth()),
                pendientes.size(),
                (int) pendientes.stream().filter(c -> c.estado() == EstadoCuenta.VENCIDA).count(),
                porCliente.values().stream().sorted(Comparator.comparing(DeudaCliente::saldo).reversed()).toList());
    }

    private Venta ventaAlCredito(Long id) {
        Venta v = ventaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta " + id));
        if (v.getFormaPago() != FormaPago.CREDITO) {
            throw new ReglaNegocioException("La venta " + v.getNumeroComprobante() + " fue al contado: no tiene cuotas por cobrar");
        }
        if (v.getEstado() != EstadoVenta.EMITIDA) {
            throw new ReglaNegocioException("La venta " + v.getNumeroComprobante() + " está anulada");
        }
        return v;
    }

    private static CalculadoraCobranza.Resultado calcular(Venta v, BigDecimal notasCredito, List<Nota> notasDebito,
                                                          BigDecimal cobrado, LocalDate hoy) {
        List<CalculadoraCobranza.Tramo> tramos = new ArrayList<>();
        v.getCuotas().forEach(c -> tramos.add(new CalculadoraCobranza.Tramo("Cuota " + c.getNumero(),
                c.getFechaVencimiento(), c.getMonto())));
        if (tramos.isEmpty()) {
            tramos.add(new CalculadoraCobranza.Tramo("Total", v.getFecha(), v.getMontoTotal()));
        }
        notasDebito.forEach(n -> tramos.add(new CalculadoraCobranza.Tramo("Nota de débito " + n.getNumero(),
                n.getFecha(), n.getMontoTotal())));
        tramos.sort(Comparator.comparing(CalculadoraCobranza.Tramo::vence));
        return CalculadoraCobranza.calcular(tramos, notasCredito, cobrado, hoy);
    }

    private static CuentaPorCobrar cuenta(Venta v, CalculadoraCobranza.Resultado r) {
        EstadoCuenta estado = r.saldo().signum() == 0 ? EstadoCuenta.PAGADA
                : r.vencido().signum() > 0 ? EstadoCuenta.VENCIDA : EstadoCuenta.AL_DIA;
        return new CuentaPorCobrar(v.getId(), v.getNumeroComprobante(), v.getFecha(), v.getCliente().getId(),
                v.getCliente().getDocumento(), v.getCliente().getNombre(), r.deuda(), r.cobrado(), r.saldo(), r.vencido(),
                r.proximoVencimiento(), r.diasMaximoAtraso(), estado);
    }

    private static CobroResponse respuesta(Cobro c) {
        return new CobroResponse(c.getId(), c.getFecha(), c.getMonto(), c.getMedio(), c.getReferencia(),
                c.getRegistradoPor(), c.getRegistradoEn());
    }
}
