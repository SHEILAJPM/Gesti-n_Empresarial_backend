package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.MarcacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock
    private PersonalService personalService;
    @Mock
    private MarcacionRepository marcacionRepository;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private Trabajador trabajador;

    @BeforeEach
    void setUp() {
        trabajador = new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", "ACTIVO");
    }

    @Test
    void marcarConDocumentoInexistenteLanzaExcepcion() {
        when(personalService.buscarPorDocumento("99999999")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> asistenciaService.marcar("99999999", TipoMarcacion.ENTRADA, null));
        verify(marcacionRepository, never()).save(any());
    }

    @Test
    void marcarConDocumentoValidoRegistraLaMarcacionLigadaAlTrabajador() {
        LocalDateTime hora = LocalDateTime.of(2026, 8, 10, 8, 0);
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        when(marcacionRepository.save(any(Marcacion.class))).thenAnswer(inv -> {
            Marcacion m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        Marcacion registrada = asistenciaService.marcar("70001234", TipoMarcacion.ENTRADA, hora);

        assertNotNull(registrada.getId());
        assertEquals(trabajador.getId(), registrada.getTrabajadorId());
        assertEquals(TipoMarcacion.ENTRADA, registrada.getTipo());
        // La hora enviada por el cliente se ignora: se usa la del servidor (America/Lima)
        assertTrue(java.time.Duration.between(registrada.getFechaHora(), LocalDateTime.now(PortalService.ZONA)).abs().getSeconds() < 5);
        assertEquals("RRHH", registrada.getOrigen());
    }

    @Test
    void marcarSinFechaUsaLaHoraActual() {
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        when(marcacionRepository.save(any(Marcacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Marcacion registrada = asistenciaService.marcar("70001234", TipoMarcacion.SALIDA, null);

        assertNotNull(registrada.getFechaHora());
    }

    @Test
    void listarPorTrabajadorDevuelveSoloSusMarcaciones() {
        when(marcacionRepository.findByTrabajadorId(1L)).thenReturn(List.of(new Marcacion(), new Marcacion()));

        List<Marcacion> marcaciones = asistenciaService.listarPorTrabajador(1L);

        assertEquals(2, marcaciones.size());
    }
}
