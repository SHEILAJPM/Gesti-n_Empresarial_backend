package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.InventarioDtos.CompraParaIngreso;
import desarrolloWeb.backend.dto.InventarioDtos.EstadoStock;
import desarrolloWeb.backend.dto.InventarioDtos.Kardex;
import desarrolloWeb.backend.dto.InventarioDtos.Linea;
import desarrolloWeb.backend.dto.InventarioDtos.Motivos;
import desarrolloWeb.backend.dto.InventarioDtos.Movimiento;
import desarrolloWeb.backend.dto.InventarioDtos.MovimientoRequest;
import desarrolloWeb.backend.dto.InventarioDtos.Operacion;
import desarrolloWeb.backend.dto.InventarioDtos.ProductoStock;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.MovimientoStock;
import desarrolloWeb.backend.entity.MovimientoStock.Motivo;
import desarrolloWeb.backend.entity.MovimientoStock.Tipo;
import desarrolloWeb.backend.entity.Producto;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.CompraRepository;
import desarrolloWeb.backend.repository.MovimientoStockRepository;
import desarrolloWeb.backend.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/* Inventario permanente: el stock de cada producto y su kárdex.
   - Las ventas descuentan solas y no se emiten si falta stock.
   - La anulación de una venta y las notas de crédito por anulación o devolución lo regresan (nunca más de lo que salió).
   - Las guías de remisión no mueven stock: la mercadería ya salió con la venta.
   - Almacén registra a mano los ingresos (compras, inventario inicial), las salidas (mermas, consumo) y los conteos. */
@Service
public class InventarioService {

    /* Compras que se ofrecen para vincular un ingreso */
    static final int DIAS_COMPRAS_RECIENTES = 120;

    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final CompraRepository compraRepository;
    private final Clock reloj = Clock.system(PortalService.ZONA);

    @PersistenceContext
    private EntityManager entityManager;

    public InventarioService(ProductoRepository productoRepository, MovimientoStockRepository movimientoRepository,
                             CompraRepository compraRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.compraRepository = compraRepository;
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<ProductoStock> listar() {
        Map<Long, LocalDateTime> ultimos = new HashMap<>();
        for (Object[] fila : movimientoRepository.ultimoMovimientoPorProducto()) {
            ultimos.put((Long) fila[0], (LocalDateTime) fila[1]);
        }
        return productoRepository.findAllByOrderByDescripcionAsc().stream()
                .filter(Producto::manejaStock)
                .map(p -> stockDe(p, ultimos.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Kardex kardex(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + productoId));
        if (!producto.manejaStock()) {
            throw new ReglaNegocioException(producto.getDescripcion() + " es un servicio: no maneja stock");
        }
        List<Movimiento> movimientos = movimientoRepository.findTop500ByProductoIdOrderByFechaDescIdDesc(productoId)
                .stream().map(InventarioService::toResponse).toList();
        BigDecimal entradas = sumar(movimientos, Tipo.ENTRADA);
        BigDecimal salidas = sumar(movimientos, Tipo.SALIDA);
        LocalDateTime ultimo = movimientos.isEmpty() ? null : movimientos.get(0).fecha();
        return new Kardex(stockDe(producto, ultimo), entradas, salidas, movimientos);
    }

    public Motivos motivos() {
        Map<Motivo, String> entrada = new EnumMap<>(Motivo.class);
        Map<Motivo, String> salida = new EnumMap<>(Motivo.class);
        for (Motivo m : Motivo.values()) {
            if (!m.isManual()) {
                continue;
            }
            if (m.admite(Tipo.ENTRADA)) {
                entrada.put(m, m.getTexto());
            }
            if (m.admite(Tipo.SALIDA)) {
                salida.put(m, m.getTexto());
            }
        }
        return new Motivos(entrada, salida);
    }

    /* Compras de los últimos meses, marcando las que ya ingresaron al almacén */
    @Transactional(readOnly = true)
    public List<CompraParaIngreso> comprasRecientes() {
        List<Compra> compras = compraRepository.recientesConProveedor(LocalDate.now(reloj).minusDays(DIAS_COMPRAS_RECIENTES));
        Set<Long> ingresadas = compras.isEmpty() ? Set.of()
                : new HashSet<>(movimientoRepository.comprasIngresadas(compras.stream().map(Compra::getId).toList()));
        return compras.stream()
                .map(c -> new CompraParaIngreso(c.getId(), c.getNumeroComprobante(), c.getProveedor().getRazonSocial(),
                        c.getFecha(), ingresadas.contains(c.getId())))
                .toList();
    }

    // ---------- Movimientos que registra Almacén ----------

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'ALMACEN')")
    @Transactional
    public List<Movimiento> registrar(MovimientoRequest datos, String usuario) {
        Operacion operacion = datos.operacion();
        Motivo motivo = motivoDe(datos);
        Compra compra = null;
        if (datos.compraId() != null) {
            if (motivo != Motivo.COMPRA) {
                throw new IllegalArgumentException("Solo los ingresos por compra se vinculan a una compra");
            }
            compra = compraRepository.findById(datos.compraId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + datos.compraId()));
        }
        String referencia = limpiar(datos.referencia());
        if (compra != null && referencia == null) {
            referencia = recortar("Compra " + compra.getNumeroComprobante() + " · " + compra.getProveedor().getRazonSocial(), 80);
        }
        if (motivo == Motivo.COMPRA && referencia == null) {
            throw new IllegalArgumentException("Elige la compra o escribe el número de guía o factura del proveedor");
        }
        String observacion = limpiar(datos.observacion());
        if (motivo == Motivo.OTRO && observacion == null) {
            throw new IllegalArgumentException("Explica en la observación por qué entra o sale la mercadería");
        }

        Map<Long, BigDecimal> cantidades = new LinkedHashMap<>();
        for (Linea l : datos.lineas()) {
            BigDecimal cantidad = l.cantidad().setScale(3, RoundingMode.HALF_UP);
            if (operacion != Operacion.AJUSTE && cantidad.signum() <= 0) {
                throw new IllegalArgumentException("Las cantidades deben ser mayores a cero");
            }
            if (cantidades.put(l.productoId(), cantidad) != null) {
                throw new IllegalArgumentException("Un producto aparece en dos líneas: júntalas en una sola");
            }
        }

        Map<Long, Producto> productos = bloquear(cantidades.keySet());
        List<String> faltantes = new ArrayList<>();
        for (Producto p : productos.values()) {
            if (!p.manejaStock()) {
                throw new IllegalArgumentException(p.getDescripcion() + " es un servicio: no maneja stock");
            }
            if (operacion == Operacion.ENTRADA && !p.isActivo()) {
                throw new IllegalArgumentException(p.getDescripcion() + " está desactivado: actívalo antes de ingresarle stock");
            }
            BigDecimal cantidad = cantidades.get(p.getId());
            if (operacion == Operacion.SALIDA && p.getStock().compareTo(cantidad) < 0) {
                faltantes.add(faltante(p, cantidad));
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("No hay stock suficiente: " + String.join("; ", faltantes));
        }

        List<Movimiento> hechos = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : cantidades.entrySet()) {
            Producto p = productos.get(e.getKey());
            BigDecimal cantidad = e.getValue();
            if (operacion == Operacion.AJUSTE) {
                // Se registra solo la diferencia entre lo contado y lo que dice el sistema
                BigDecimal diferencia = cantidad.subtract(p.getStock());
                if (diferencia.signum() == 0) {
                    continue;
                }
                hechos.add(toResponse(mover(p, diferencia.signum() > 0 ? Tipo.ENTRADA : Tipo.SALIDA, Motivo.AJUSTE,
                        diferencia.abs(), null, null, referencia, observacion, usuario)));
            } else {
                Tipo tipo = operacion == Operacion.ENTRADA ? Tipo.ENTRADA : Tipo.SALIDA;
                hechos.add(toResponse(mover(p, tipo, motivo, cantidad, null, compra, referencia, observacion, usuario)));
            }
        }
        return hechos;
    }

    // ---------- Movimientos automáticos (ventas y notas de crédito) ----------

    /* Venta emitida: descuenta lo vendido. Si algún producto no alcanza, se rechaza la venta completa
       (se llama antes de enviarla a SUNAT, dentro de la misma transacción). */
    @Transactional
    public void registrarVenta(Venta venta) {
        Map<Long, BigDecimal> vendido = new TreeMap<>();
        Map<Long, String> unidades = new HashMap<>();
        for (VentaItem i : venta.getItems()) {
            if (i.getProductoId() != null) {
                vendido.merge(i.getProductoId(), i.getCantidad(), BigDecimal::add);
                unidades.put(i.getProductoId(), i.getUnidad());
            }
        }
        if (vendido.isEmpty()) {
            return;
        }
        Map<Long, Producto> productos = bloquear(vendido.keySet());
        productos.values().removeIf(p -> !p.manejaStock());
        List<String> faltantes = new ArrayList<>();
        for (Producto p : productos.values()) {
            if (!p.getUnidad().equals(unidades.get(p.getId()))) {
                throw new IllegalArgumentException("La línea de " + p.getDescripcion() + " está en " + unidades.get(p.getId())
                        + " pero su stock se controla en " + p.getUnidad() + ": usa la misma unidad");
            }
            BigDecimal cantidad = vendido.get(p.getId());
            if (p.getStock().compareTo(cantidad) < 0) {
                faltantes.add(faltante(p, cantidad));
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("No hay stock suficiente para emitir " + venta.getNumeroComprobante() + ": "
                    + String.join("; ", faltantes) + ". Almacén debe registrar el ingreso o reduce las cantidades.");
        }
        String usuario = usuarioActual();
        for (Producto p : productos.values()) {
            mover(p, Tipo.SALIDA, Motivo.VENTA, vendido.get(p.getId()), venta, null, venta.getNumeroComprobante(), null,
                    usuario);
        }
    }

    /* Anulación de la venta o nota de crédito que la revierte entera: regresa todo lo que aún no había vuelto */
    @Transactional
    public void devolverVenta(Venta venta, Motivo motivo, String referencia) {
        Saldos saldos = saldosDe(venta);
        Map<Long, BigDecimal> pendiente = new TreeMap<>();
        for (Long id : saldos.vendido().keySet()) {
            BigDecimal cantidad = saldos.pendiente(id);
            if (cantidad.signum() > 0) {
                pendiente.put(id, cantidad);
            }
        }
        regresar(venta, pendiente, motivo, referencia);
    }

    /* Devolución por ítem: regresa lo indicado sin superar lo que salió con la venta y aún no volvió.
       Los productos que no salieron del almacén con esta venta (servicios, ventas anteriores al inventario) se omiten. */
    @Transactional
    public void devolverItems(Venta venta, Map<Long, BigDecimal> cantidades, String referencia) {
        Saldos saldos = saldosDe(venta);
        Map<Long, BigDecimal> aDevolver = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> e : cantidades.entrySet()) {
            if (!saldos.vendido().containsKey(e.getKey()) || e.getValue().signum() <= 0) {
                continue;
            }
            BigDecimal pendiente = saldos.pendiente(e.getKey());
            if (e.getValue().compareTo(pendiente) > 0) {
                String nombre = productoRepository.findById(e.getKey()).map(Producto::getDescripcion).orElse("el producto");
                throw new IllegalArgumentException("De " + nombre + " salieron " + texto(saldos.vendido().get(e.getKey()))
                        + " con " + venta.getNumeroComprobante() + " y ya volvieron "
                        + texto(saldos.devuelto().getOrDefault(e.getKey(), BigDecimal.ZERO))
                        + ": puedes devolver hasta " + texto(pendiente));
            }
            aDevolver.put(e.getKey(), e.getValue());
        }
        regresar(venta, aDevolver, Motivo.DEVOLUCION_CLIENTE, referencia);
    }

    // ---------- Internos ----------

    private void regresar(Venta venta, Map<Long, BigDecimal> cantidades, Motivo motivo, String referencia) {
        if (cantidades.isEmpty()) {
            return;
        }
        Map<Long, Producto> productos = bloquear(cantidades.keySet());
        String usuario = usuarioActual();
        for (Map.Entry<Long, BigDecimal> e : cantidades.entrySet()) {
            mover(productos.get(e.getKey()), Tipo.ENTRADA, motivo, e.getValue(), venta, null, referencia, null, usuario);
        }
    }

    private record Saldos(Map<Long, BigDecimal> vendido, Map<Long, BigDecimal> devuelto) {
        BigDecimal pendiente(Long productoId) {
            return vendido.getOrDefault(productoId, BigDecimal.ZERO)
                    .subtract(devuelto.getOrDefault(productoId, BigDecimal.ZERO));
        }
    }

    /* Lo que salió con la venta y lo que ya volvió, por producto, según el kárdex */
    private Saldos saldosDe(Venta venta) {
        Map<Long, BigDecimal> vendido = new TreeMap<>();
        Map<Long, BigDecimal> devuelto = new TreeMap<>();
        for (MovimientoStock m : movimientoRepository.findByVentaId(venta.getId())) {
            (m.getTipo() == Tipo.SALIDA ? vendido : devuelto)
                    .merge(m.getProducto().getId(), m.getCantidad(), BigDecimal::add);
        }
        return new Saldos(vendido, devuelto);
    }

    /* Bloquea las filas de los productos con stock y relee su saldo. Siempre en orden de id, para que dos ventas
       simultáneas de los mismos productos no se esperen mutuamente. */
    private Map<Long, Producto> bloquear(Collection<Long> ids) {
        // Lo pendiente se escribe antes: refresh descarta los cambios que aún no llegaron a la base
        entityManager.flush();
        Map<Long, Producto> productos = new TreeMap<>();
        for (Long id : new TreeSet<>(ids)) {
            Producto p = productoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + id));
            if (p.manejaStock()) {
                entityManager.refresh(p, LockModeType.PESSIMISTIC_WRITE);
            }
            productos.put(id, p);
        }
        return productos;
    }

    private MovimientoStock mover(Producto p, Tipo tipo, Motivo motivo, BigDecimal cantidad, Venta venta, Compra compra,
                                  String referencia, String observacion, String usuario) {
        BigDecimal saldo = (tipo == Tipo.ENTRADA ? p.getStock().add(cantidad) : p.getStock().subtract(cantidad))
                .setScale(3, RoundingMode.HALF_UP);
        if (saldo.signum() < 0) {
            throw new ReglaNegocioException("No hay stock suficiente: " + faltante(p, cantidad));
        }
        p.setStock(saldo);
        MovimientoStock m = new MovimientoStock();
        m.setProducto(p);
        m.setFecha(LocalDateTime.now(reloj));
        m.setTipo(tipo);
        m.setMotivo(motivo);
        m.setCantidad(cantidad.setScale(3, RoundingMode.HALF_UP));
        m.setSaldo(saldo);
        m.setVenta(venta);
        m.setCompra(compra);
        m.setReferencia(referencia);
        m.setObservacion(observacion);
        m.setRegistradoPor(usuario);
        return movimientoRepository.save(m);
    }

    private static Motivo motivoDe(MovimientoRequest datos) {
        if (datos.operacion() == Operacion.AJUSTE) {
            return Motivo.AJUSTE;
        }
        Tipo tipo = datos.operacion() == Operacion.ENTRADA ? Tipo.ENTRADA : Tipo.SALIDA;
        Motivo motivo = datos.motivo();
        if (motivo == null || !motivo.isManual() || !motivo.admite(tipo)) {
            throw new IllegalArgumentException("Elige un motivo válido para " + (tipo == Tipo.ENTRADA ? "el ingreso" : "la salida"));
        }
        return motivo;
    }

    static EstadoStock estado(Producto p) {
        if (p.getStock().signum() <= 0) {
            return EstadoStock.SIN_STOCK;
        }
        if (p.getStockMinimo().signum() > 0 && p.getStock().compareTo(p.getStockMinimo()) <= 0) {
            return EstadoStock.BAJO;
        }
        return EstadoStock.NORMAL;
    }

    private static ProductoStock stockDe(Producto p, LocalDateTime ultimoMovimiento) {
        return new ProductoStock(p.getId(), p.getCodigo(), p.getDescripcion(), p.getUnidad(), p.isActivo(), p.getStock(),
                p.getStockMinimo(), estado(p), ultimoMovimiento);
    }

    private static Movimiento toResponse(MovimientoStock m) {
        Producto p = m.getProducto();
        return new Movimiento(m.getId(), m.getFecha(), m.getTipo(), m.getMotivo(), m.getMotivo().getTexto(),
                m.getCantidad(), m.getSaldo(), m.getReferencia(), m.getObservacion(), m.getRegistradoPor(),
                m.getVenta() == null ? null : m.getVenta().getId(), m.getCompra() == null ? null : m.getCompra().getId(),
                p.getId(), p.getDescripcion());
    }

    private static BigDecimal sumar(List<Movimiento> movimientos, Tipo tipo) {
        return movimientos.stream().filter(m -> m.tipo() == tipo).map(Movimiento::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(3, RoundingMode.HALF_UP);
    }

    private static String faltante(Producto p, BigDecimal pedido) {
        return p.getCodigo() + " " + p.getDescripcion() + " (hay " + texto(p.getStock()) + ", se necesitan " + texto(pedido) + ")";
    }

    /* 2.000 → "2"; 1.250 → "1.25" */
    static String texto(BigDecimal cantidad) {
        return cantidad.signum() == 0 ? "0" : cantidad.stripTrailingZeros().toPlainString();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a instanceof AnonymousAuthenticationToken ? null : a.getName();
    }
}
