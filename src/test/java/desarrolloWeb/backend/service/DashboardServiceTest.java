package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CompraResponse;
import desarrolloWeb.backend.dto.DashboardMetricas;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.dto.VentaResponse;
import desarrolloWeb.backend.entity.Documento;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.Trabajador;
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
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock
    private PersonalService personalService;
    @Mock
    private AsistenciaService asistenciaService;
    @Mock
    private PagoService pagoService;
    @Mock
    private DocumentoService documentoService;
    @Mock
    private CompraService compraService;
    @Mock
    private VentaService ventaService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("las métricas reflejan lo que devuelve cada módulo")
    void obtenerMetricasReflejaLoRegistradoEnCadaModulo() {
        TrabajadorResponse trabajador = TrabajadorResponse.de(
                new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", "ACTIVO"));
        when(personalService.listar(null, null)).thenReturn(List.of(trabajador));
        when(personalService.listar(null, "ACTIVO")).thenReturn(List.of(trabajador));
        when(asistenciaService.listarTodas()).thenReturn(List.of(new Marcacion()));
        when(pagoService.listarPendientes()).thenReturn(List.of(new Pago(), new Pago()));
        when(documentoService.listarProximosAVencer(30)).thenReturn(List.of(new Documento()));
        when(documentoService.listarVencidos()).thenReturn(List.of());
        when(compraService.listarTodas()).thenReturn(List.of(compra()));
        when(ventaService.listarTodas()).thenReturn(List.of(venta()));

        DashboardMetricas metricas = dashboardService.obtenerMetricas();

        assertAll(
                () -> assertEquals(1, metricas.totalTrabajadores()),
                () -> assertEquals(1, metricas.trabajadoresActivos()),
                () -> assertEquals(1, metricas.totalMarcaciones()),
                () -> assertEquals(2, metricas.pagosPendientes()),
                () -> assertEquals(1, metricas.documentosProximosAVencer()),
                () -> assertEquals(0, metricas.documentosVencidos()),
                () -> assertEquals(1, metricas.totalCompras()),
                () -> assertEquals(1, metricas.totalVentas())
        );
    }

    private CompraResponse compra() {
        return new CompraResponse(1L, 1L, "20123456789", "Distribuidora ACME S.A.C.", "F001-1",
                LocalDate.of(2026, 8, 10), new BigDecimal("100.00"), new BigDecimal("18.00"),
                new BigDecimal("118.00"), new BigDecimal("0.1800"));
    }

    private VentaResponse venta() {
        return new VentaResponse(1L, 1L, "10203040", "Carlos Mendoza", TipoComprobante.BOLETA, "B001-1",
                LocalDate.of(2026, 8, 15), new BigDecimal("300.00"), new BigDecimal("54.00"),
                new BigDecimal("354.00"), new BigDecimal("0.1800"), EstadoVenta.EMITIDA);
    }
}
