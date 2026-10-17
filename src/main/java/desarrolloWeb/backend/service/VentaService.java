package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.TicketPromedioResponse;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.dto.VentasPorComprobante;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.CalculadoraTributaria;
import desarrolloWeb.backend.util.ValidadorDocumento;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ConfiguracionTributariaService configuracionService;
    private final FacturacionService facturacionService;
    private final CalculadoraTributaria calculadora = new CalculadoraTributaria();
    private final ValidadorDocumento validador = new ValidadorDocumento();

    public VentaService(VentaRepository ventaRepository,
                        ClienteRepository clienteRepository,
                        ConfiguracionTributariaService configuracionService,
                        FacturacionService facturacionService) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.configuracionService = configuracionService;
        this.facturacionService = facturacionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION')")
    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        Cliente cliente = clienteRepository.findByDocumento(request.documentoCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un cliente con documento " + request.documentoCliente()));

        TipoComprobante tipo = validador.tipoComprobantePara(cliente.getDocumento());
        if (!request.numeroComprobante().startsWith(tipo.getPrefijo())) {
            throw new DocumentoInvalidoException("El cliente con " + cliente.getTipoDocumento()
                    + " requiere " + tipo + ": el comprobante debe empezar con '" + tipo.getPrefijo() + "'");
        }

        if (ventaRepository.existsByNumeroComprobante(request.numeroComprobante())) {
            throw new ReglaNegocioException("Ya existe una venta con el comprobante " + request.numeroComprobante());
        }

        BigDecimal tasa = configuracionService.obtenerTasaVigente(ConfiguracionTributariaService.CODIGO_IGV);
        BigDecimal base = request.montoSinIgv().setScale(2, RoundingMode.HALF_UP);

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setTipoComprobante(tipo);
        venta.setNumeroComprobante(request.numeroComprobante());
        venta.setFecha(request.fecha());
        venta.setMontoSinIgv(base);
        venta.setIgv(calculadora.calcularIgv(base, tasa));
        venta.setMontoTotal(calculadora.calcularTotal(base, tasa));
        venta.setTasaAplicada(tasa);
        venta.setEstado(EstadoVenta.EMITIDA);

        Venta guardada = ventaRepository.save(venta);
        // Se envía como comprobante electrónico; si el proveedor falla, la venta queda registrada con estado ERROR
        facturacionService.emitir(guardada);
        return toResponse(guardada);
    }

    /* Consulta a SUNAT/APISUNAT el estado actual del comprobante */
    @Transactional
    public VentaResponse consultarSunat(Long id) {
        Venta venta = obtenerEntidad(id);
        facturacionService.consultar(venta);
        return toResponse(venta);
    }

    /* Reintenta el envío de un comprobante que falló o fue rechazado */
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION')")
    @Transactional
    public VentaResponse reenviarSunat(Long id) {
        Venta venta = obtenerEntidad(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("No se envía a SUNAT una venta anulada");
        }
        facturacionService.emitir(venta);
        return toResponse(venta);
    }

    /* Siguiente número libre de la serie F001 o B001, considerando lo ya emitido en el proveedor */
    @Transactional(readOnly = true)
    public String siguienteNumero(TipoComprobante tipo) {
        String serie = tipo.getPrefijo() + "001";
        int local = ventaRepository.findAll().stream()
                .map(Venta::getNumeroComprobante)
                .filter(n -> n.matches(serie + "-\\d{1,8}"))
                .mapToInt(n -> Integer.parseInt(n.substring(serie.length() + 1)))
                .max().orElse(0) + 1;
        String codigoTipo = tipo == TipoComprobante.FACTURA ? "01" : "03";
        return serie + "-" + facturacionService.siguienteCorrelativo(codigoTipo, serie, local);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarTodas() {
        return mapear(ventaRepository.findAll());
    }

    @Transactional(readOnly = true)
    public VentaResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorCliente(Long clienteId) {
        return mapear(ventaRepository.buscarPorCliente(clienteId));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return mapear(ventaRepository.buscarPorPeriodo(desde, hasta));
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalIgvPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Venta venta : ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, desde, hasta)) {
            total = total.add(venta.getIgv());
        }
        return total;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CONTABILIDAD')")
    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = obtenerEntidad(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("La venta " + venta.getNumeroComprobante() + " ya está anulada");
        }

        venta.setEstado(EstadoVenta.ANULADA);
        facturacionService.anular(venta, "Anulación de la operación");
        return toResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentasPorComprobante> contarPorComprobante() {
        List<VentasPorComprobante> reporte = new ArrayList<>();
        for (TipoComprobante tipo : TipoComprobante.values()) {
            reporte.add(new VentasPorComprobante(tipo,
                    ventaRepository.contarPorTipoComprobante(tipo, EstadoVenta.EMITIDA)));
        }
        return reporte;
    }

    @Transactional(readOnly = true)
    public TicketPromedioResponse calcularTicketPromedio(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Double promedio = ventaRepository.promedioMontoSinIgv(EstadoVenta.EMITIDA, desde, hasta);

        BigDecimal ticket = promedio == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(promedio).setScale(2, RoundingMode.HALF_UP);
        return new TicketPromedioResponse(desde, hasta, ticket);
    }

    private Venta obtenerEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una venta con id " + id));
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private List<VentaResponse> mapear(List<Venta> ventas) {
        List<VentaResponse> respuesta = new ArrayList<>();
        for (Venta venta : ventas) {
            respuesta.add(toResponse(venta));
        }
        return respuesta;
    }

    private VentaResponse toResponse(Venta venta) {
        return new VentaResponse(
                venta.getId(),
                venta.getCliente().getId(),
                venta.getCliente().getDocumento(),
                venta.getCliente().getNombre(),
                venta.getTipoComprobante(),
                venta.getNumeroComprobante(),
                venta.getFecha(),
                venta.getMontoSinIgv(),
                venta.getIgv(),
                venta.getMontoTotal(),
                venta.getTasaAplicada(),
                venta.getEstado(),
                facturacionService.respuesta(venta));
    }
}
