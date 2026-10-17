package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ResumenTributario;
import desarrolloWeb.backend.dto.SimulacionResponse;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.repository.CompraRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TributarioService")
class TributarioServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ConfiguracionTributariaService configuracionService;

    @InjectMocks
    private TributarioService tributarioService;

    @Test
    @DisplayName("agosto: compra S/ 100 y venta S/ 300 dan crédito 18.00, débito 54.00 y por pagar 36.00")
    void resumenDeAgosto() {
        when(compraRepository.buscarPorPeriodo(DESDE, HASTA)).thenReturn(List.of(compra("100.00", "18.00")));
        when(ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA))
                .thenReturn(List.of(venta("300.00", "54.00")));
        when(compraRepository.contarPorPeriodo(DESDE, HASTA)).thenReturn(1L);
        when(ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(1L);

        ResumenTributario resumen = tributarioService.generarResumen(DESDE, HASTA);

        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), resumen.totalComprasSinIgv()),
                () -> assertEquals(new BigDecimal("18.00"), resumen.igvCompras()),
                () -> assertEquals(new BigDecimal("300.00"), resumen.totalVentasSinIgv()),
                () -> assertEquals(new BigDecimal("54.00"), resumen.igvVentas()),
                () -> assertEquals(new BigDecimal("36.00"), resumen.igvPorPagar()),
                () -> assertEquals(new BigDecimal("0.00"), resumen.saldoAFavor()),
                () -> assertEquals(1L, resumen.cantidadCompras()),
                () -> assertEquals(1L, resumen.cantidadVentas())
        );
    }

    @Test
    @DisplayName("si el crédito supera al débito, igvPorPagar es 0.00 y la diferencia es saldo a favor")
    void saldoAFavor() {
        when(compraRepository.buscarPorPeriodo(DESDE, HASTA)).thenReturn(List.of(compra("500.00", "90.00")));
        when(ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA))
                .thenReturn(List.of(venta("300.00", "54.00")));
        when(compraRepository.contarPorPeriodo(DESDE, HASTA)).thenReturn(1L);
        when(ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(1L);

        ResumenTributario resumen = tributarioService.generarResumen(DESDE, HASTA);

        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), resumen.igvPorPagar()),
                () -> assertEquals(new BigDecimal("36.00"), resumen.saldoAFavor())
        );
    }

    @Test
    @DisplayName("un periodo sin movimientos devuelve todo en 0.00")
    void periodoVacio() {
        when(compraRepository.buscarPorPeriodo(DESDE, HASTA)).thenReturn(List.of());
        when(ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(List.of());
        when(compraRepository.contarPorPeriodo(DESDE, HASTA)).thenReturn(0L);
        when(ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(0L);

        ResumenTributario resumen = tributarioService.generarResumen(DESDE, HASTA);

        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), resumen.totalComprasSinIgv()),
                () -> assertEquals(new BigDecimal("0.00"), resumen.igvPorPagar()),
                () -> assertEquals(0L, resumen.cantidadVentas())
        );
    }

    @Test
    @DisplayName("el resumen mensual de agosto usa del 1 al 31")
    void resumenMensual() {
        when(compraRepository.buscarPorPeriodo(DESDE, HASTA)).thenReturn(List.of());
        when(ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(List.of());
        when(compraRepository.contarPorPeriodo(DESDE, HASTA)).thenReturn(0L);
        when(ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, DESDE, HASTA)).thenReturn(0L);

        ResumenTributario resumen = tributarioService.generarResumenMensual(2026, 8);

        assertAll(
                () -> assertEquals(DESDE, resumen.desde()),
                () -> assertEquals(HASTA, resumen.hasta())
        );
    }

    @Test
    @DisplayName("un mes fuera de 1 a 12 lanza IllegalArgumentException")
    void mesInvalido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> tributarioService.generarResumenMensual(2026, 13));

        assertEquals("El mes debe estar entre 1 y 12", error.getMessage());
    }

    @Test
    @DisplayName("el simulador desglosa S/ 118.00 con IGV en 100.00 + 18.00")
    void simularConIgv() {
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));

        SimulacionResponse simulacion = tributarioService.simular(new BigDecimal("118.00"), true);

        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), simulacion.base()),
                () -> assertEquals(new BigDecimal("18.00"), simulacion.igv()),
                () -> assertEquals(new BigDecimal("118.00"), simulacion.total())
        );
    }

    @Test
    @DisplayName("el simulador suma el IGV a S/ 100.00 sin IGV")
    void simularSinIgv() {
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));

        SimulacionResponse simulacion = tributarioService.simular(new BigDecimal("100"), false);

        assertAll(
                () -> assertEquals(new BigDecimal("18.00"), simulacion.igv()),
                () -> assertEquals(new BigDecimal("118.00"), simulacion.total())
        );
    }

    private Compra compra(String base, String igv) {
        Compra compra = new Compra();
        compra.setMontoSinIgv(new BigDecimal(base));
        compra.setIgv(new BigDecimal(igv));
        return compra;
    }

    private Venta venta(String base, String igv) {
        Venta venta = new Venta();
        venta.setMontoSinIgv(new BigDecimal(base));
        venta.setIgv(new BigDecimal(igv));
        venta.setEstado(EstadoVenta.EMITIDA);
        return venta;
    }
}
