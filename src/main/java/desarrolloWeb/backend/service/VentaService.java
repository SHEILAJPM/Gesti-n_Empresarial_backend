package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.TicketPromedioResponse;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.dto.VentasPorComprobante;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.FormaPago;
import desarrolloWeb.backend.entity.MovimientoStock;
import desarrolloWeb.backend.entity.Producto;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaCuota;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.repository.ProductoRepository;
import desarrolloWeb.backend.util.UnidadMedida;
import desarrolloWeb.backend.util.MontoEnLetras;
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
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final CalculadoraTributaria calculadora = new CalculadoraTributaria();
    private final ValidadorDocumento validador = new ValidadorDocumento();

    public VentaService(VentaRepository ventaRepository,
                        ClienteRepository clienteRepository,
                        ConfiguracionTributariaService configuracionService,
                        FacturacionService facturacionService,
                        ProductoRepository productoRepository,
                        InventarioService inventarioService) {
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.configuracionService = configuracionService;
        this.facturacionService = facturacionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'VENDEDOR')")
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
            // Otra persona registró ese número mientras se llenaba el formulario: se sugiere el siguiente libre
            throw new ReglaNegocioException("Ya existe una venta con el comprobante " + request.numeroComprobante()
                    + ". El siguiente número libre es " + siguienteNumero(tipo));
        }

        BigDecimal tasa = configuracionService.obtenerTasaVigente(ConfiguracionTributariaService.CODIGO_IGV);
        List<VentaItem> lineas = construirLineas(request.items(), tasa);
        BigDecimal base;
        if (!lineas.isEmpty()) {
            base = lineas.stream().map(VentaItem::getValorVenta).reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (request.montoSinIgv() != null) {
            base = request.montoSinIgv().setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Agrega al menos un producto o servicio al comprobante");
        }
        if (base.signum() <= 0) {
            throw new IllegalArgumentException("El total del comprobante debe ser mayor a cero");
        }
        BigDecimal total = calculadora.calcularTotal(base, tasa);

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setTipoComprobante(tipo);
        venta.setNumeroComprobante(request.numeroComprobante());
        venta.setFecha(request.fecha());
        venta.setMontoSinIgv(base);
        venta.setIgv(calculadora.calcularIgv(base, tasa));
        venta.setMontoTotal(total);
        venta.setTasaAplicada(tasa);
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setObservaciones(request.observaciones() == null || request.observaciones().isBlank()
                ? null : request.observaciones().trim());
        lineas.forEach(venta::agregarItem);
        aplicarFormaPago(venta, tipo, request, total);

        Venta guardada = ventaRepository.save(venta);
        // Descuenta el stock antes de enviar: si falta mercadería se revierte todo y SUNAT no recibe nada
        inventarioService.registrarVenta(guardada);
        // Se envía como comprobante electrónico; si el proveedor falla, la venta queda registrada con estado ERROR
        facturacionService.emitir(guardada);
        return toResponse(guardada, true);
    }

    /* Líneas del comprobante: valor de venta = cantidad × precio sin IGV; el IGV de cada línea se redondea aparte */
    private List<VentaItem> construirLineas(List<VentaRequest.Item> items, BigDecimal tasa) {
        if (items == null) {
            return List.of();
        }
        List<VentaItem> lineas = new ArrayList<>();
        for (VentaRequest.Item i : items) {
            VentaItem linea = new VentaItem();
            String codigo = i.codigo() == null || i.codigo().isBlank() ? null : i.codigo().trim().toUpperCase();
            if (i.productoId() != null) {
                Producto producto = productoRepository.findById(i.productoId())
                        .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + i.productoId()));
                if (!producto.isActivo()) {
                    throw new IllegalArgumentException("El producto " + producto.getCodigo() + " está desactivado");
                }
                linea.setProductoId(producto.getId());
                if (codigo == null) {
                    codigo = producto.getCodigo();
                }
            }
            BigDecimal cantidad = i.cantidad().setScale(3, RoundingMode.HALF_UP);
            BigDecimal valorUnitario = i.valorUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal valorVenta = cantidad.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
            linea.setCodigo(codigo);
            linea.setDescripcion(i.descripcion().trim());
            linea.setUnidad(UnidadMedida.validar(i.unidad()));
            linea.setCantidad(cantidad);
            linea.setValorUnitario(valorUnitario);
            linea.setValorVenta(valorVenta);
            linea.setIgv(calculadora.calcularIgv(valorVenta, tasa));
            lineas.add(linea);
        }
        return lineas;
    }

    /* Al crédito: solo con factura, con cuotas posteriores a la emisión que sumen el total (exigencia de SUNAT) */
    private static void aplicarFormaPago(Venta venta, TipoComprobante tipo, VentaRequest request, BigDecimal total) {
        FormaPago forma = request.formaPago() == null ? FormaPago.CONTADO : request.formaPago();
        venta.setFormaPago(forma);
        if (forma == FormaPago.CONTADO) {
            return;
        }
        if (tipo != TipoComprobante.FACTURA) {
            throw new IllegalArgumentException("Las ventas al crédito se emiten con factura (cliente con RUC)");
        }
        List<VentaRequest.Cuota> cuotas = request.cuotas() == null ? List.of() : request.cuotas();
        if (cuotas.isEmpty()) {
            throw new IllegalArgumentException("Una venta al crédito necesita al menos una cuota");
        }
        BigDecimal suma = BigDecimal.ZERO;
        LocalDate anterior = request.fecha();
        for (VentaRequest.Cuota c : cuotas) {
            if (!c.fechaVencimiento().isAfter(anterior)) {
                throw new IllegalArgumentException("Cada cuota debe vencer después de la fecha de emisión y de la cuota anterior");
            }
            anterior = c.fechaVencimiento();
            VentaCuota cuota = new VentaCuota();
            cuota.setFechaVencimiento(c.fechaVencimiento());
            cuota.setMonto(c.monto().setScale(2, RoundingMode.HALF_UP));
            venta.agregarCuota(cuota);
            suma = suma.add(cuota.getMonto());
        }
        if (suma.compareTo(total) != 0) {
            throw new IllegalArgumentException("Las cuotas suman S/ " + suma + " y deben sumar el total de S/ " + total);
        }
    }

    /* Consulta a SUNAT/APISUNAT el estado actual del comprobante */
    @Transactional
    public VentaResponse consultarSunat(Long id) {
        Venta venta = obtenerEntidad(id);
        facturacionService.consultar(venta);
        return toResponse(venta);
    }

    /* Reintenta el envío de un comprobante que falló o fue rechazado */
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'VENDEDOR')")
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
        // El máximo se calcula en la base: no hace falta traer todas las ventas
        Long maximo = ventaRepository.maximoCorrelativo(serie + "-", serie.length() + 2);
        int local = (maximo == null ? 0 : Math.toIntExact(maximo)) + 1;
        String codigoTipo = tipo == TipoComprobante.FACTURA ? "01" : "03";
        return serie + "-" + facturacionService.siguienteCorrelativo(codigoTipo, serie, local);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarTodas() {
        return mapear(ventaRepository.listarConCliente());
    }

    /* Con el detalle completo: líneas y cuotas */
    @Transactional(readOnly = true)
    public VentaResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id), true);
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
        // La mercadería vuelve al almacén (solo lo que no regresó antes con notas de crédito)
        inventarioService.devolverVenta(venta, MovimientoStock.Motivo.ANULACION_VENTA, venta.getNumeroComprobante() + " anulada");
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
        return toResponse(venta, false);
    }

    /* completo = con líneas y cuotas; el listado no las carga para no hacer una consulta por venta */
    private VentaResponse toResponse(Venta venta, boolean completo) {
        List<VentaResponse.Item> items = !completo ? List.of() : venta.getItems().stream()
                .map(i -> new VentaResponse.Item(i.getOrden(), i.getProductoId(), i.getCodigo(), i.getDescripcion(),
                        i.getUnidad(), i.getCantidad(), i.getValorUnitario(), i.getValorVenta(), i.getIgv()))
                .toList();
        List<VentaResponse.Cuota> cuotas = !completo ? List.of() : venta.getCuotas().stream()
                .map(c -> new VentaResponse.Cuota(c.getNumero(), c.getFechaVencimiento(), c.getMonto()))
                .toList();
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
                facturacionService.respuesta(venta),
                venta.getFormaPago(),
                venta.getObservaciones(),
                items.size(),
                items,
                cuotas,
                completo ? MontoEnLetras.convertir(venta.getMontoTotal()) : null);
    }
}
