package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.InventarioDtos.CompraParaIngreso;
import desarrolloWeb.backend.dto.InventarioDtos.EstadoStock;
import desarrolloWeb.backend.dto.InventarioDtos.Kardex;
import desarrolloWeb.backend.dto.InventarioDtos.Linea;
import desarrolloWeb.backend.dto.InventarioDtos.Movimiento;
import desarrolloWeb.backend.dto.InventarioDtos.MovimientoRequest;
import desarrolloWeb.backend.dto.InventarioDtos.Operacion;
import desarrolloWeb.backend.dto.InventarioDtos.ProductoStock;
import desarrolloWeb.backend.dto.ProductoRequest;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.MovimientoStock.Motivo;
import desarrolloWeb.backend.entity.MovimientoStock.Tipo;
import desarrolloWeb.backend.entity.Producto;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.entity.VentaItem;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.CompraRepository;
import desarrolloWeb.backend.repository.MovimientoStockRepository;
import desarrolloWeb.backend.repository.ProductoRepository;
import desarrolloWeb.backend.repository.ProveedorRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Contra H2: las ventas y notas de crédito mueven el stock sin dejarlo negativo ni devolver de más */
@DataJpaTest
@Import({InventarioService.class, ProductoService.class})
@DisplayName("Inventario y kárdex")
class InventarioServiceTest {

    @Autowired private InventarioService inventario;
    @Autowired private ProductoService productoService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private MovimientoStockRepository movimientoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CompraRepository compraRepository;

    private Producto laptop;
    private Producto mouse;
    private Producto instalacion;
    private Cliente cliente;
    private int correlativo;

    @BeforeEach
    void preparar() {
        laptop = producto("LAP-01", "Laptop 14", "NIU", "2");
        mouse = producto("MOU-01", "Mouse inalámbrico", "NIU", "0");
        instalacion = producto("INS-01", "Instalación", "ZZ", "0");
        cliente = clienteRepository.save(new Cliente("20555666777", "Comercial Lima S.A.C.", TipoDocumento.RUC));
    }

    private Producto producto(String codigo, String descripcion, String unidad, String minimo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setDescripcion(descripcion);
        p.setUnidad(unidad);
        p.setValorUnitario(new BigDecimal("100.00"));
        p.setStockMinimo(new BigDecimal(minimo));
        return productoRepository.save(p);
    }

    private List<Movimiento> mover(Operacion operacion, Motivo motivo, Producto p, String cantidad) {
        return inventario.registrar(new MovimientoRequest(operacion, motivo, null, null, "Prueba",
                List.of(new Linea(p.getId(), new BigDecimal(cantidad)))), "almacen1");
    }

    private void ingresar(Producto p, String cantidad) {
        mover(Operacion.ENTRADA, Motivo.SALDO_INICIAL, p, cantidad);
    }

    /* Venta guardada con sus líneas: producto + cantidad (la unidad es la del producto salvo que se indique) */
    private Venta venta(Object... lineas) {
        Venta v = new Venta();
        v.setCliente(cliente);
        v.setTipoComprobante(TipoComprobante.FACTURA);
        v.setNumeroComprobante("F001-" + (++correlativo));
        v.setFecha(LocalDate.now());
        v.setMontoSinIgv(new BigDecimal("100.00"));
        v.setIgv(new BigDecimal("18.00"));
        v.setMontoTotal(new BigDecimal("118.00"));
        v.setTasaAplicada(new BigDecimal("0.1800"));
        v.setEstado(EstadoVenta.EMITIDA);
        for (int i = 0; i < lineas.length; i += 2) {
            Producto p = (Producto) lineas[i];
            VentaItem item = new VentaItem();
            item.setProductoId(p.getId());
            item.setCodigo(p.getCodigo());
            item.setDescripcion(p.getDescripcion());
            item.setUnidad(p.getUnidad());
            item.setCantidad(new BigDecimal((String) lineas[i + 1]));
            item.setValorUnitario(new BigDecimal("100.00"));
            item.setValorVenta(new BigDecimal("100.00"));
            item.setIgv(new BigDecimal("18.00"));
            v.agregarItem(item);
        }
        return ventaRepository.save(v);
    }

    private BigDecimal stock(Producto p) {
        return productoRepository.findById(p.getId()).orElseThrow().getStock().setScale(3);
    }

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor).setScale(3);
    }

    @Test
    @DisplayName("la venta descuenta lo vendido y deja el saldo en el kárdex; los servicios no se tocan")
    void laVentaDescuentaElStock() {
        ingresar(laptop, "5");
        Venta venta = venta(laptop, "2", instalacion, "1");

        inventario.registrarVenta(venta);

        assertEquals(n("3"), stock(laptop));
        assertEquals(n("0"), stock(instalacion));
        Kardex kardex = inventario.kardex(laptop.getId());
        assertEquals(2, kardex.movimientos().size());
        Movimiento salida = kardex.movimientos().get(0);
        assertEquals(Tipo.SALIDA, salida.tipo());
        assertEquals(Motivo.VENTA, salida.motivo());
        assertEquals(n("3"), salida.saldo());
        assertEquals(venta.getNumeroComprobante(), salida.referencia());
        assertEquals(venta.getId(), salida.ventaId());
        assertEquals(n("5"), kardex.entradas());
        assertEquals(n("2"), kardex.salidas());
    }

    @Test
    @DisplayName("si un producto no alcanza no se mueve ninguno y el mensaje dice cuánto hay")
    void sinStockNoSeMueveNada() {
        ingresar(laptop, "1");
        ingresar(mouse, "5");
        Venta venta = venta(laptop, "2", mouse, "1");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> inventario.registrarVenta(venta));

        assertTrue(error.getMessage().contains("LAP-01 Laptop 14 (hay 1, se necesitan 2)"), error.getMessage());
        assertEquals(n("5"), stock(mouse));
        assertTrue(movimientoRepository.findByVentaId(venta.getId()).isEmpty());
    }

    @Test
    @DisplayName("la línea debe usar la misma unidad con la que se controla el stock")
    void laUnidadDebeCoincidir() {
        ingresar(laptop, "50");
        Venta venta = venta(laptop, "1");
        venta.getItems().get(0).setUnidad("DZN");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> inventario.registrarVenta(venta));
        assertTrue(error.getMessage().contains("se controla en NIU"), error.getMessage());
    }

    @Test
    @DisplayName("devolución por ítem y anulación regresan solo lo que salió y aún no volvió")
    void lasDevolucionesNoDuplicanStock() {
        ingresar(laptop, "5");
        Venta venta = venta(laptop, "3");
        inventario.registrarVenta(venta);

        inventario.devolverItems(venta, Map.of(laptop.getId(), n("1")), "FC01-1");
        assertEquals(n("3"), stock(laptop));

        inventario.devolverVenta(venta, Motivo.ANULACION_VENTA, "F001-1 anulada");
        assertEquals(n("5"), stock(laptop));

        // Una segunda anulación no tiene nada pendiente
        inventario.devolverVenta(venta, Motivo.ANULACION_VENTA, "F001-1 anulada");
        assertEquals(n("5"), stock(laptop));
        assertEquals(3, movimientoRepository.findByVentaId(venta.getId()).size());
    }

    @Test
    @DisplayName("no se devuelve más de lo que salió con la venta")
    void noSeDevuelveDeMas() {
        ingresar(laptop, "5");
        Venta venta = venta(laptop, "2");
        inventario.registrarVenta(venta);
        inventario.devolverItems(venta, Map.of(laptop.getId(), n("1")), "FC01-1");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> inventario.devolverItems(venta, Map.of(laptop.getId(), n("2")), "FC01-2"));
        assertTrue(error.getMessage().contains("puedes devolver hasta 1"), error.getMessage());
    }

    @Test
    @DisplayName("una venta anterior al inventario no mueve stock al devolverse")
    void ventaAnteriorAlInventario() {
        Venta antigua = venta(laptop, "2");

        inventario.devolverItems(antigua, Map.of(laptop.getId(), n("1")), "FC01-1");
        inventario.devolverVenta(antigua, Motivo.ANULACION_VENTA, "anulada");

        assertEquals(n("0"), stock(laptop));
        assertTrue(movimientoRepository.findByVentaId(antigua.getId()).isEmpty());
    }

    @Test
    @DisplayName("el conteo físico registra solo la diferencia, hacia arriba o hacia abajo")
    void ajustePorConteo() {
        ingresar(laptop, "10");

        List<Movimiento> faltan = mover(Operacion.AJUSTE, null, laptop, "7");
        assertEquals(1, faltan.size());
        assertEquals(Tipo.SALIDA, faltan.get(0).tipo());
        assertEquals(n("3"), faltan.get(0).cantidad());

        List<Movimiento> sobran = mover(Operacion.AJUSTE, null, laptop, "9");
        assertEquals(Tipo.ENTRADA, sobran.get(0).tipo());
        assertEquals(n("9"), sobran.get(0).saldo());

        assertTrue(mover(Operacion.AJUSTE, null, laptop, "9").isEmpty(), "si el conteo coincide no hay movimiento");
        assertEquals(n("9"), stock(laptop));
    }

    @Test
    @DisplayName("una salida no puede dejar el stock negativo")
    void salidaSinStock() {
        ingresar(mouse, "2");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> mover(Operacion.SALIDA, Motivo.MERMA, mouse, "3"));
        assertTrue(error.getMessage().contains("hay 2"), error.getMessage());
    }

    @Test
    @DisplayName("los motivos de ventas y devoluciones no se registran a mano, ni se mueve stock de servicios")
    void motivosYServicios() {
        assertThrows(IllegalArgumentException.class, () -> mover(Operacion.ENTRADA, Motivo.DEVOLUCION_CLIENTE, laptop, "1"));
        assertThrows(IllegalArgumentException.class, () -> mover(Operacion.SALIDA, Motivo.COMPRA, laptop, "1"));
        assertThrows(IllegalArgumentException.class, () -> mover(Operacion.ENTRADA, null, laptop, "1"));
        IllegalArgumentException servicio = assertThrows(IllegalArgumentException.class, () -> ingresar(instalacion, "1"));
        assertTrue(servicio.getMessage().contains("es un servicio"));
        assertThrows(IllegalArgumentException.class, () -> inventario.registrar(new MovimientoRequest(Operacion.ENTRADA,
                Motivo.OTRO, null, null, null, List.of(new Linea(laptop.getId(), BigDecimal.ONE))), "almacen1"),
                "«Otro» necesita una explicación");
    }

    @Test
    @DisplayName("el ingreso por compra queda vinculado y la compra aparece como ingresada")
    void ingresoPorCompra() {
        Proveedor proveedor = proveedorRepository.save(new Proveedor("20123456789", "Distribuidora ACME S.A.C.", null));
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setNumeroComprobante("F001-123");
        compra.setFecha(LocalDate.now());
        compra.setMontoSinIgv(new BigDecimal("1000.00"));
        compra.setIgv(new BigDecimal("180.00"));
        compra.setMontoTotal(new BigDecimal("1180.00"));
        compra.setTasaAplicada(new BigDecimal("0.1800"));
        compra = compraRepository.save(compra);

        CompraParaIngreso antes = inventario.comprasRecientes().get(0);
        assertFalse(antes.ingresada());

        List<Movimiento> hechos = inventario.registrar(new MovimientoRequest(Operacion.ENTRADA, Motivo.COMPRA, compra.getId(),
                null, null, List.of(new Linea(laptop.getId(), new BigDecimal("4")), new Linea(mouse.getId(), new BigDecimal("10")))),
                "almacen1");

        assertEquals(2, hechos.size());
        assertEquals("Compra F001-123 · Distribuidora ACME S.A.C.", hechos.get(0).referencia());
        assertEquals(compra.getId(), hechos.get(0).compraId());
        assertEquals(n("10"), stock(mouse));
        assertTrue(inventario.comprasRecientes().get(0).ingresada());
        assertTrue(movimientoRepository.existsByCompraId(compra.getId()));
    }

    @Test
    @DisplayName("sin stock, con stock bajo el mínimo o normal; los servicios no se listan")
    void estadoDelStock() {
        assertEquals(EstadoStock.SIN_STOCK, estadoDe(laptop));
        ingresar(laptop, "2");
        assertEquals(EstadoStock.BAJO, estadoDe(laptop));
        ingresar(laptop, "1");
        assertEquals(EstadoStock.NORMAL, estadoDe(laptop));
        // Sin mínimo definido, cualquier stock positivo es normal
        ingresar(mouse, "1");
        assertEquals(EstadoStock.NORMAL, estadoDe(mouse));
        assertTrue(inventario.listar().stream().noneMatch(p -> p.id().equals(instalacion.getId())));
    }

    private EstadoStock estadoDe(Producto p) {
        ProductoStock fila = inventario.listar().stream().filter(x -> x.id().equals(p.getId())).findFirst().orElseThrow();
        return fila.estado();
    }

    @Test
    @DisplayName("con stock no se cambia la unidad del producto")
    void noSeCambiaLaUnidadConStock() {
        ingresar(laptop, "1");
        ProductoRequest enKilos = new ProductoRequest("LAP-01", "Laptop 14", "KGM", new BigDecimal("100.00"));

        assertThrows(ReglaNegocioException.class, () -> productoService.actualizar(laptop.getId(), enKilos));

        // El mouse no tiene stock: sí puede cambiar de unidad, y conserva su mínimo si no se envía
        mouse.setStockMinimo(new BigDecimal("4"));
        productoService.actualizar(mouse.getId(), new ProductoRequest("MOU-01", "Mouse", "PK", new BigDecimal("30.00")));
        assertEquals("PK", productoRepository.findById(mouse.getId()).orElseThrow().getUnidad());
        assertEquals(new BigDecimal("4"), productoRepository.findById(mouse.getId()).orElseThrow().getStockMinimo());
    }
}
