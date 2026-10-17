package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.PlanillaResponse;
import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.Planilla;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.PlanillaRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanillaServiceTest {

    @Mock
    private PlanillaRepository planillaRepository;
    @Mock
    private TrabajadorRepository trabajadorRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoService pagoService;

    @InjectMocks
    private PlanillaService planillaService;

    private static final YearMonth PERIODO = YearMonth.of(2026, 9);

    private static Trabajador trabajador(long id, String nombre, String dni, String sueldo, String sistema, String afp) {
        Trabajador t = new Trabajador(id, nombre, dni, "Analista", "Ventas", Trabajador.ACTIVO);
        t.setFechaIngreso(LocalDate.of(2024, 1, 1));
        t.setSueldo(sueldo == null ? null : new BigDecimal(sueldo));
        t.setSistemaPension(sistema);
        t.setAfp(afp);
        return t;
    }

    @Test
    void generarIncluyeFichasCompletasYReportaLasOmitidas() {
        when(planillaRepository.findByAnioAndMes(2026, 9)).thenReturn(Optional.empty());
        when(trabajadorRepository.findAll()).thenReturn(List.of(
                trabajador(1, "Ana Torres", "70000001", "2500.00", "ONP", null),
                trabajador(2, "Luis Ramos", "70000002", "3000.00", "AFP", "Prima"),
                trabajador(3, "Rosa Díaz", "70000003", null, "ONP", null)));
        when(planillaRepository.save(any(Planilla.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanillaResponse r = planillaService.generar(PERIODO.getYear(), PERIODO.getMonthValue(), "rrhh1");

        assertEquals(EstadoPlanilla.BORRADOR, r.estado());
        assertEquals(2, r.cantidadTrabajadores());
        assertEquals(new BigDecimal("5500.00"), r.totalBruto());
        // ONP 13 % = 325.00 + AFP Prima (10 % + 1.37 % + 1.60 % = 12.97 %) = 389.10
        assertEquals(new BigDecimal("714.10"), r.totalDescuentos());
        assertEquals(new BigDecimal("4785.90"), r.totalNeto());
        assertEquals(List.of("Rosa Díaz: sin sueldo"), r.omitidos());
    }

    @Test
    void regenerarUnBorradorLoReemplaza() {
        Planilla borrador = new Planilla();
        borrador.setEstado(EstadoPlanilla.BORRADOR);
        when(planillaRepository.findByAnioAndMes(2026, 9)).thenReturn(Optional.of(borrador));
        when(trabajadorRepository.findAll()).thenReturn(List.of(trabajador(1, "Ana Torres", "70000001", "2500.00", "ONP", null)));
        when(planillaRepository.save(any(Planilla.class))).thenAnswer(inv -> inv.getArgument(0));

        planillaService.generar(2026, 9, "rrhh1");

        verify(planillaRepository).delete(borrador);
    }

    @Test
    void noSeRegeneraUnaPlanillaAprobada() {
        Planilla aprobada = new Planilla();
        aprobada.setEstado(EstadoPlanilla.APROBADA);
        when(planillaRepository.findByAnioAndMes(2026, 9)).thenReturn(Optional.of(aprobada));

        assertThrows(ReglaNegocioException.class, () -> planillaService.generar(2026, 9, "rrhh1"));
        verify(planillaRepository, never()).delete(any());
    }

    @Test
    void noSeGeneraUnMesFuturo() {
        YearMonth siguiente = YearMonth.now().plusMonths(1);
        assertThrows(ReglaNegocioException.class,
                () -> planillaService.generar(siguiente.getYear(), siguiente.getMonthValue(), "rrhh1"));
    }

    @Test
    void sinFichasCompletasNoSeGeneraPlanilla() {
        when(planillaRepository.findByAnioAndMes(2026, 9)).thenReturn(Optional.empty());
        when(trabajadorRepository.findAll()).thenReturn(List.of(trabajador(1, "Ana Torres", "70000001", "2500.00", null, null)));

        assertThrows(ReglaNegocioException.class, () -> planillaService.generar(2026, 9, "rrhh1"));
    }

    @Test
    void aprobarCreaUnPagoPorTrabajadorConElNeto() {
        when(planillaRepository.findByAnioAndMes(2026, 9)).thenReturn(Optional.empty());
        when(trabajadorRepository.findAll()).thenReturn(List.of(
                trabajador(1, "Ana Torres", "70000001", "2500.00", "ONP", null),
                trabajador(2, "Luis Ramos", "70000002", "3000.00", "AFP", "Prima")));
        when(planillaRepository.save(any(Planilla.class))).thenAnswer(inv -> inv.getArgument(0));
        planillaService.generar(2026, 9, "rrhh1");
        org.mockito.ArgumentCaptor<Planilla> captor = org.mockito.ArgumentCaptor.forClass(Planilla.class);
        verify(planillaRepository).save(captor.capture());
        Planilla generada = captor.getValue();
        when(planillaRepository.buscarParaActualizar(5L)).thenReturn(Optional.of(generada));
        when(pagoService.registrar(any(String.class), any(String.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    Pago pago = new Pago();
                    pago.setId(inv.getArgument(0, String.class).endsWith("1") ? 11L : 12L);
                    return pago;
                });

        PlanillaResponse r = planillaService.aprobar(5L, LocalDate.of(2026, 9, 30), "contabilidad1");

        assertEquals(EstadoPlanilla.APROBADA, r.estado());
        assertEquals("contabilidad1", r.aprobadaPor());
        verify(pagoService, times(2)).registrar(any(String.class), eq("Sueldo setiembre 2026"), any(BigDecimal.class), eq(LocalDate.of(2026, 9, 30)));
        verify(pagoService).registrar(eq("70000001"), any(String.class), eq(new BigDecimal("2175.00")), any(LocalDate.class));
        assertTrue(r.detalles().stream().allMatch(d -> d.pagoId() != null));
    }

    @Test
    void noSeApruebaDosVecesNiSeEliminaAprobada() {
        Planilla aprobada = new Planilla();
        aprobada.setEstado(EstadoPlanilla.APROBADA);
        when(planillaRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(aprobada));

        assertThrows(ReglaNegocioException.class, () -> planillaService.aprobar(1L, LocalDate.of(2026, 9, 30), "contabilidad1"));
        assertThrows(ReglaNegocioException.class, () -> planillaService.eliminar(1L));
        verify(pagoService, never()).registrar(any(String.class), any(String.class), any(BigDecimal.class), any(LocalDate.class));
    }
}
