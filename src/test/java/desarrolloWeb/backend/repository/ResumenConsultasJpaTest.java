package desarrolloWeb.backend.repository;

import desarrolloWeb.backend.dto.ResumenTributario;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.service.ConfiguracionTributariaService;
import desarrolloWeb.backend.service.TributarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TributarioService.class, ConfiguracionTributariaService.class})
@DisplayName("Consultas JPQL del resumen tributario")
class ResumenConsultasJpaTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private TributarioService tributarioService;

    private Proveedor proveedor;
    private Cliente clienteDni;
    private Cliente clienteRuc;

    @BeforeEach
    void setUp() {
        proveedor = proveedorRepository.save(new Proveedor("20123456789", "Distribuidora ACME S.A.C.", null));
        clienteDni = clienteRepository.save(new Cliente("10203040", "Carlos Mendoza", TipoDocumento.DNI));
        clienteRuc = clienteRepository.save(new Cliente("20555666777", "Comercial Lima S.A.C.", TipoDocumento.RUC));

        compraRepository.save(compra("F001-1", LocalDate.of(2026, 8, 10), "100.00", "18.00"));
        compraRepository.save(compra("F001-2", LocalDate.of(2026, 7, 20), "250.00", "45.00"));
        ventaRepository.save(venta(clienteDni, TipoComprobante.BOLETA, "B001-1",
                LocalDate.of(2026, 8, 15), "300.00", "54.00", EstadoVenta.EMITIDA));
        ventaRepository.save(venta(clienteRuc, TipoComprobante.FACTURA, "F001-1",
                LocalDate.of(2026, 8, 20), "200.00", "36.00", EstadoVenta.ANULADA));
        ventaRepository.save(venta(clienteRuc, TipoComprobante.FACTURA, "F001-2",
                LocalDate.of(2026, 9, 5), "500.00", "90.00", EstadoVenta.EMITIDA));
    }

    @Test
    @DisplayName("BETWEEN de compras devuelve solo agosto")
    void comprasEntreFechas() {
        List<Compra> compras = compraRepository.buscarPorPeriodo(DESDE, HASTA);

        assertAll(
                () -> assertEquals(1, compras.size()),
                () -> assertEquals("F001-1", compras.get(0).getNumeroComprobante())
        );
    }

    @Test
    @DisplayName("las ventas por estado excluyen las ANULADAS y se ordenan por fecha DESC")
    void ventasEmitidasExcluyenAnuladas() {
        List<Venta> todas = ventaRepository.buscarPorPeriodo(DESDE, HASTA);
        List<Venta> emitidas = ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA);

        assertAll(
                () -> assertEquals(2, todas.size()),
                () -> assertEquals(LocalDate.of(2026, 8, 20), todas.get(0).getFecha()),
                () -> assertEquals(1, emitidas.size()),
                () -> assertEquals("B001-1", emitidas.get(0).getNumeroComprobante())
        );
    }

    @Test
    @DisplayName("COUNT por periodo y por tipo de comprobante")
    void conteos() {
        assertAll(
                () -> assertEquals(1L, compraRepository.contarPorPeriodo(DESDE, HASTA)),
                () -> assertEquals(1L, ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)),
                () -> assertEquals(1L, ventaRepository.contarPorTipoComprobante(
                        TipoComprobante.FACTURA, EstadoVenta.EMITIDA)),
                () -> assertEquals(1L, ventaRepository.contarPorTipoComprobante(
                        TipoComprobante.BOLETA, EstadoVenta.EMITIDA))
        );
    }

    @Test
    @DisplayName("AVG del monto devuelve Double con el promedio de las ventas EMITIDAS")
    void promedio() {
        Double promedio = ventaRepository.promedioMontoSinIgv(
                EstadoVenta.EMITIDA, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertEquals(400.0, promedio, 0.001);
    }

    @Test
    @DisplayName("LIKE encuentra clientes por parte del nombre sin importar mayúsculas")
    void buscarClientesPorTexto() {
        List<Cliente> encontrados = clienteRepository.buscarPorTexto("LIMA");

        assertAll(
                () -> assertEquals(1, encontrados.size()),
                () -> assertEquals("20555666777", encontrados.get(0).getDocumento())
        );
    }

    @Test
    @DisplayName("el resumen real de agosto da crédito 18.00, débito 54.00 y por pagar 36.00")
    void resumenDeAgosto() {
        ResumenTributario resumen = tributarioService.generarResumen(DESDE, HASTA);

        assertAll(
                () -> assertEquals(0, new BigDecimal("18.00").compareTo(resumen.igvCompras())),
                () -> assertEquals(0, new BigDecimal("54.00").compareTo(resumen.igvVentas())),
                () -> assertEquals(0, new BigDecimal("36.00").compareTo(resumen.igvPorPagar())),
                () -> assertEquals(0, BigDecimal.ZERO.compareTo(resumen.saldoAFavor())),
                () -> assertEquals(1L, resumen.cantidadCompras()),
                () -> assertEquals(1L, resumen.cantidadVentas())
        );
    }

    @Test
    @DisplayName("la restricción UNIQUE (proveedor_id, numero_comprobante) rechaza duplicados")
    void restriccionUnicaDeCompras() {
        assertThrows(DataIntegrityViolationException.class,
                () -> compraRepository.saveAndFlush(compra("F001-1", LocalDate.of(2026, 8, 11), "10.00", "1.80")));
    }

    private Compra compra(String numero, LocalDate fecha, String base, String igv) {
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setNumeroComprobante(numero);
        compra.setFecha(fecha);
        compra.setMontoSinIgv(new BigDecimal(base));
        compra.setIgv(new BigDecimal(igv));
        compra.setMontoTotal(new BigDecimal(base).add(new BigDecimal(igv)));
        compra.setTasaAplicada(new BigDecimal("0.1800"));
        return compra;
    }

    private Venta venta(Cliente cliente, TipoComprobante tipo, String numero, LocalDate fecha,
                        String base, String igv, EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setTipoComprobante(tipo);
        venta.setNumeroComprobante(numero);
        venta.setFecha(fecha);
        venta.setMontoSinIgv(new BigDecimal(base));
        venta.setIgv(new BigDecimal(igv));
        venta.setMontoTotal(new BigDecimal(base).add(new BigDecimal(igv)));
        venta.setTasaAplicada(new BigDecimal("0.1800"));
        venta.setEstado(estado);
        return venta;
    }
}
