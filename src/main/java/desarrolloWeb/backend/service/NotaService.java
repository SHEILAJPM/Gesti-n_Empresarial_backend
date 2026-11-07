package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.NotaRequest;
import desarrolloWeb.backend.dto.NotaResponse;
import desarrolloWeb.backend.dto.VentaRequest;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.MovimientoStock;
import desarrolloWeb.backend.entity.Nota;
import desarrolloWeb.backend.entity.NotaItem;
import desarrolloWeb.backend.entity.TipoNota;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.NotaRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.CalculadoraTributaria;
import desarrolloWeb.backend.util.MontoEnLetras;
import desarrolloWeb.backend.util.MotivoNota;
import desarrolloWeb.backend.util.UnidadMedida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/* Notas de crédito y débito. Una nota modifica una factura o boleta ya aceptada por SUNAT:
   - crédito: resta (anulación, devolución, descuento). Nunca puede superar el saldo del comprobante.
   - débito: suma (intereses por mora, aumento de valor, penalidades). */
@Service
public class NotaService {

    private final NotaRepository notaRepository;
    private final VentaRepository ventaRepository;
    private final FacturacionService facturacionService;
    private final InventarioService inventarioService;
    private final CalculadoraTributaria calculadora = new CalculadoraTributaria();
    private final Clock reloj = Clock.system(PortalService.ZONA);

    public NotaService(NotaRepository notaRepository, VentaRepository ventaRepository,
                       FacturacionService facturacionService, InventarioService inventarioService) {
        this.notaRepository = notaRepository;
        this.ventaRepository = ventaRepository;
        this.facturacionService = facturacionService;
        this.inventarioService = inventarioService;
    }

    @Transactional
    public NotaResponse emitir(NotaRequest datos, String usuario) {
        Venta venta = ventaRepository.findById(datos.ventaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta " + datos.ventaId()));
        if (venta.getEstado() != EstadoVenta.EMITIDA) {
            throw new ReglaNegocioException("La venta " + venta.getNumeroComprobante() + " está anulada");
        }
        if (venta.getSunat().getEstado() != EstadoSunat.ACEPTADO) {
            throw new ReglaNegocioException("La nota solo se emite sobre un comprobante aceptado por SUNAT. "
                    + venta.getNumeroComprobante() + " está en estado " + venta.getSunat().getEstado());
        }
        TipoNota tipo = datos.tipo();
        String motivo = MotivoNota.validar(tipo, datos.motivoCodigo());
        LocalDate fecha = datos.fecha() != null ? datos.fecha() : LocalDate.now(reloj);
        if (fecha.isBefore(venta.getFecha())) {
            throw new IllegalArgumentException("La nota no puede tener fecha anterior al comprobante (" + venta.getFecha() + ")");
        }

        BigDecimal tasa = venta.getTasaAplicada();
        BigDecimal saldo = saldo(venta);
        List<NotaItem> lineas;
        if (MotivoNota.esTotal(tipo, motivo)) {
            if (saldo.signum() <= 0) {
                throw new ReglaNegocioException("El comprobante " + venta.getNumeroComprobante() + " ya no tiene saldo");
            }
            if (saldo.compareTo(venta.getMontoTotal()) != 0) {
                throw new ReglaNegocioException("El comprobante ya tiene notas de crédito o débito: para revertir el resto "
                        + "usa una devolución o disminución por ítem por el saldo de S/ " + saldo);
            }
            lineas = copiarLineas(venta);
        } else {
            lineas = construirLineas(datos.items(), tasa);
        }

        BigDecimal base = lineas.stream().map(NotaItem::getValorVenta).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (base.signum() <= 0) {
            throw new IllegalArgumentException("El importe de la nota debe ser mayor a cero");
        }
        BigDecimal total = calculadora.calcularTotal(base, tasa);
        if (tipo == TipoNota.CREDITO && total.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("La nota de crédito (S/ " + total + ") supera el saldo del comprobante (S/ "
                    + saldo + ")");
        }

        Nota nota = new Nota();
        nota.setTipo(tipo);
        nota.setVenta(venta);
        nota.setNumero(siguienteNumero(tipo, venta));
        nota.setFecha(fecha);
        nota.setMotivoCodigo(motivo);
        nota.setSustento(datos.sustento().trim());
        nota.setMontoSinIgv(base);
        nota.setIgv(calculadora.calcularIgv(base, tasa));
        nota.setMontoTotal(total);
        nota.setTasaAplicada(tasa);
        nota.setRegistradaPor(usuario);
        lineas.forEach(nota::agregarItem);

        Nota guardada = notaRepository.save(nota);
        // La mercadería devuelta vuelve al almacén antes de enviar la nota: si algo no cuadra no se emite
        if (MotivoNota.esTotal(tipo, motivo)) {
            inventarioService.devolverVenta(venta, MotivoNota.DEVOLUCION_TOTAL.equals(motivo)
                    ? MovimientoStock.Motivo.DEVOLUCION_CLIENTE : MovimientoStock.Motivo.ANULACION_VENTA, guardada.getNumero());
        } else if (MotivoNota.esDevolucionPorItem(tipo, motivo)) {
            inventarioService.devolverItems(venta, productosDevueltos(venta, datos.items()), guardada.getNumero());
        }
        facturacionService.emitir(guardada);
        return toResponse(guardada, true);
    }

    /* Productos que regresan en una devolución por ítem: por su id o, si no viene, por el código de la línea vendida */
    private static Map<Long, BigDecimal> productosDevueltos(Venta venta, List<VentaRequest.Item> items) {
        Set<Long> vendidos = new HashSet<>();
        Map<String, Long> porCodigo = new HashMap<>();
        for (VentaItem i : venta.getItems()) {
            if (i.getProductoId() != null) {
                vendidos.add(i.getProductoId());
                if (i.getCodigo() != null) {
                    porCodigo.putIfAbsent(i.getCodigo().toUpperCase(), i.getProductoId());
                }
            }
        }
        Map<Long, BigDecimal> devueltos = new TreeMap<>();
        for (VentaRequest.Item i : items) {
            Long id = i.productoId();
            if (id == null && i.codigo() != null && !i.codigo().isBlank()) {
                id = porCodigo.get(i.codigo().trim().toUpperCase());
            }
            if (id == null) {
                continue;
            }
            if (!vendidos.contains(id)) {
                throw new IllegalArgumentException("La línea «" + i.descripcion().trim() + "» no es un producto de "
                        + venta.getNumeroComprobante() + ": solo se devuelve lo que se vendió");
            }
            devueltos.merge(id, i.cantidad().setScale(3, RoundingMode.HALF_UP), BigDecimal::add);
        }
        return devueltos;
    }

    @Transactional(readOnly = true)
    public List<NotaResponse> listar() {
        return notaRepository.listarConVenta().stream().map(n -> toResponse(n, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<NotaResponse> listarPorVenta(Long ventaId) {
        return notaRepository.findByVentaIdOrderByIdAsc(ventaId).stream().map(n -> toResponse(n, false)).toList();
    }

    @Transactional(readOnly = true)
    public NotaResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id), true);
    }

    @Transactional
    public NotaResponse consultarSunat(Long id) {
        Nota nota = obtenerEntidad(id);
        facturacionService.consultar(nota);
        return toResponse(nota, true);
    }

    @Transactional
    public NotaResponse reenviarSunat(Long id) {
        Nota nota = obtenerEntidad(id);
        facturacionService.emitir(nota);
        return toResponse(nota, true);
    }

    /* Lo que aún se le puede descontar al comprobante: total + débitos − créditos */
    @Transactional(readOnly = true)
    public BigDecimal saldo(Venta venta) {
        return venta.getMontoTotal()
                .add(notaRepository.totalPorVentaYTipo(venta.getId(), TipoNota.DEBITO))
                .subtract(notaRepository.totalPorVentaYTipo(venta.getId(), TipoNota.CREDITO))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Nota obtenerEntidad(Long id) {
        return notaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la nota " + id));
    }

    /* FC01-1, FC01-2... por serie; el proveedor puede indicar un correlativo mayor si ya emitió en su portal */
    private String siguienteNumero(TipoNota tipo, Venta venta) {
        String serie = tipo.serie(venta.getTipoComprobante());
        Long maximo = notaRepository.maximoCorrelativo(serie + "-", serie.length() + 2);
        int local = (maximo == null ? 0 : Math.toIntExact(maximo)) + 1;
        return serie + "-" + facturacionService.siguienteCorrelativo(tipo.getCodigoSunat(), serie, local);
    }

    /* Anulación o devolución total: las mismas líneas de la venta (o una genérica si la venta no tenía detalle) */
    private static List<NotaItem> copiarLineas(Venta venta) {
        List<NotaItem> lineas = new ArrayList<>();
        if (venta.getItems().isEmpty()) {
            lineas.add(linea(null, "Venta de bienes y/o servicios", "ZZ", BigDecimal.ONE.setScale(3),
                    venta.getMontoSinIgv(), venta.getMontoSinIgv(), venta.getIgv()));
            return lineas;
        }
        for (VentaItem i : venta.getItems()) {
            lineas.add(linea(i.getCodigo(), i.getDescripcion(), i.getUnidad(), i.getCantidad(), i.getValorUnitario(),
                    i.getValorVenta(), i.getIgv()));
        }
        return lineas;
    }

    private List<NotaItem> construirLineas(List<VentaRequest.Item> items, BigDecimal tasa) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos una línea con el importe de la nota");
        }
        List<NotaItem> lineas = new ArrayList<>();
        for (VentaRequest.Item i : items) {
            BigDecimal cantidad = i.cantidad().setScale(3, RoundingMode.HALF_UP);
            BigDecimal valorUnitario = i.valorUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal valorVenta = cantidad.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
            String codigo = i.codigo() == null || i.codigo().isBlank() ? null : i.codigo().trim().toUpperCase();
            lineas.add(linea(codigo, i.descripcion().trim(), UnidadMedida.validar(i.unidad()), cantidad, valorUnitario,
                    valorVenta, calculadora.calcularIgv(valorVenta, tasa)));
        }
        return lineas;
    }

    private static NotaItem linea(String codigo, String descripcion, String unidad, BigDecimal cantidad,
                                  BigDecimal valorUnitario, BigDecimal valorVenta, BigDecimal igv) {
        NotaItem item = new NotaItem();
        item.setCodigo(codigo);
        item.setDescripcion(descripcion);
        item.setUnidad(unidad);
        item.setCantidad(cantidad);
        item.setValorUnitario(valorUnitario);
        item.setValorVenta(valorVenta);
        item.setIgv(igv);
        return item;
    }

    private NotaResponse toResponse(Nota n, boolean completo) {
        Venta v = n.getVenta();
        List<VentaResponse.Item> items = !completo ? List.of() : n.getItems().stream()
                .map(i -> new VentaResponse.Item(i.getOrden(), null, i.getCodigo(), i.getDescripcion(), i.getUnidad(),
                        i.getCantidad(), i.getValorUnitario(), i.getValorVenta(), i.getIgv()))
                .toList();
        return new NotaResponse(n.getId(), n.getTipo(), n.getNumero(), n.getFecha(), n.getMotivoCodigo(),
                MotivoNota.de(n.getTipo()).get(n.getMotivoCodigo()), n.getSustento(), v.getId(), v.getTipoComprobante(),
                v.getNumeroComprobante(), v.getCliente().getDocumento(), v.getCliente().getNombre(), n.getMontoSinIgv(),
                n.getIgv(), n.getMontoTotal(), n.getTasaAplicada(), n.getRegistradaPor(), facturacionService.respuesta(n),
                items, completo ? MontoEnLetras.convertir(n.getMontoTotal()) : null);
    }
}
