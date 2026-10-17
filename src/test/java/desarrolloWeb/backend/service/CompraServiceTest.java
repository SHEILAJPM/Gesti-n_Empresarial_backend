package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CompraRequest;
import desarrolloWeb.backend.dto.CompraResponse;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.CompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompraService")
class CompraServiceTest {

    private static final String RUC = "20123456789";
    private static final LocalDate FECHA = LocalDate.of(2026, 8, 10);

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private ProveedorService proveedorService;
    @Mock
    private PagoService pagoService;
    @Mock
    private ConfiguracionTributariaService configuracionService;

    @InjectMocks
    private CompraService compraService;

    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        proveedor = new Proveedor(RUC, "Distribuidora ACME S.A.C.", null);
        proveedor.setId(1L);
    }

    @Test
    @DisplayName("registrar calcula IGV 18.00 y total 118.00 con la tasa vigente y programa el pago")
    void registrarCalculaIgvYProgramaPago() {
        when(proveedorService.buscarPorRuc(RUC)).thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorIdAndNumeroComprobante(1L, "F001-1")).thenReturn(Optional.empty());
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));
        when(compraRepository.saveAndFlush(any(Compra.class))).thenAnswer(inv -> {
            Compra compra = inv.getArgument(0);
            compra.setId(10L);
            return compra;
        });

        CompraResponse respuesta = compraService.registrar(
                new CompraRequest(RUC, "F001-1", new BigDecimal("100"), FECHA), false);

        assertAll(
                () -> assertEquals(10L, respuesta.id()),
                () -> assertEquals(new BigDecimal("100.00"), respuesta.montoSinIgv()),
                () -> assertEquals(new BigDecimal("18.00"), respuesta.igv()),
                () -> assertEquals(new BigDecimal("118.00"), respuesta.montoTotal()),
                () -> assertEquals(new BigDecimal("0.1800"), respuesta.tasaAplicada())
        );
        verify(pagoService).registrarProveedor(RUC, "Compra F001-1", 118.0, FECHA);
    }

    @Test
    @DisplayName("registrar con un RUC que no existe lanza 404 y no guarda nada")
    void registrarConRucInexistente() {
        when(proveedorService.buscarPorRuc(RUC)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> compraService.registrar(new CompraRequest(RUC, "F001-1", BigDecimal.TEN, FECHA), false));

        assertEquals("No existe un proveedor con RUC " + RUC, error.getMessage());
        verify(compraRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("registrar un comprobante repetido del mismo proveedor lanza 409")
    void registrarComprobanteDuplicado() {
        when(proveedorService.buscarPorRuc(RUC)).thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorIdAndNumeroComprobante(1L, "F001-1"))
                .thenReturn(Optional.of(new Compra()));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> compraService.registrar(new CompraRequest(RUC, "F001-1", BigDecimal.TEN, FECHA), false));

        assertEquals("El proveedor " + RUC + " ya tiene registrada la compra F001-1", error.getMessage());
    }

    @Test
    @DisplayName("simularError lanza la excepción después del INSERT y no programa el pago")
    void simularErrorLanzaDespuesDelSave() {
        when(proveedorService.buscarPorRuc(RUC)).thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorIdAndNumeroComprobante(1L, "F001-9")).thenReturn(Optional.empty());
        when(configuracionService.obtenerTasaVigente("IGV")).thenReturn(new BigDecimal("0.1800"));
        when(compraRepository.saveAndFlush(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> compraService.registrar(new CompraRequest(RUC, "F001-9", BigDecimal.TEN, FECHA), true));

        assertTrue(error.getMessage().startsWith("ERROR SIMULADO"));
        verify(compraRepository).saveAndFlush(any(Compra.class));
        verify(pagoService, never()).registrarProveedor(anyString(), anyString(), anyDouble(), any());
    }

    @Test
    @DisplayName("actualizar recalcula con la tasa histórica y no llama a save (dirty checking)")
    void actualizarUsaTasaHistoricaSinSave() {
        Compra existente = new Compra();
        existente.setId(5L);
        existente.setProveedor(proveedor);
        existente.setTasaAplicada(new BigDecimal("0.1000"));
        when(compraRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(proveedorService.buscarPorRuc(RUC)).thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorIdAndNumeroComprobante(1L, "F001-2")).thenReturn(Optional.empty());

        CompraResponse respuesta = compraService.actualizar(5L,
                new CompraRequest(RUC, "F001-2", new BigDecimal("200.00"), FECHA));

        assertAll(
                () -> assertEquals(new BigDecimal("20.00"), respuesta.igv()),
                () -> assertEquals(new BigDecimal("220.00"), respuesta.montoTotal()),
                () -> assertEquals("F001-2", respuesta.numeroComprobante())
        );
        verify(compraRepository, never()).save(any());
        verify(configuracionService, never()).obtenerTasaVigente(anyString());
    }

    @Test
    @DisplayName("calcularTotalIgvPeriodo suma el IGV guardado en cada compra")
    void calcularTotalIgvPeriodo() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);
        when(compraRepository.buscarPorPeriodo(desde, hasta))
                .thenReturn(List.of(compraConIgv("18.00"), compraConIgv("36.00")));

        assertEquals(new BigDecimal("54.00"), compraService.calcularTotalIgvPeriodo(desde, hasta));
    }

    @Test
    @DisplayName("un periodo con 'desde' posterior a 'hasta' lanza IllegalArgumentException")
    void periodoInvalido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> compraService.calcularTotalIgvPeriodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)));

        assertEquals("La fecha 'desde' no puede ser posterior a 'hasta'", error.getMessage());
    }

    @Test
    @DisplayName("buscarPorId inexistente lanza 404 con mensaje en español")
    void buscarPorIdInexistente() {
        when(compraRepository.findById(99L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> compraService.buscarPorId(99L));

        assertEquals("No existe una compra con id 99", error.getMessage());
    }

    @Test
    @DisplayName("eliminar una compra existente la borra del repositorio")
    void eliminarCompraExistente() {
        Compra existente = new Compra();
        when(compraRepository.findById(5L)).thenReturn(Optional.of(existente));

        compraService.eliminar(5L);

        verify(compraRepository).delete(eq(existente));
    }

    private Compra compraConIgv(String igv) {
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setIgv(new BigDecimal(igv));
        return compra;
    }
}
