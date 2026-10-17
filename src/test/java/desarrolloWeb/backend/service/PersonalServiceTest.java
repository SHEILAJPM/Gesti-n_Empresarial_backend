package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CesarTrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.DocumentoRepository;
import desarrolloWeb.backend.repository.MarcacionRepository;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalServiceTest {

    @Mock
    private TrabajadorRepository trabajadorRepository;
    @Mock
    private MarcacionRepository marcacionRepository;
    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PersonalService personalService;

    private static TrabajadorRequest ficha(String documento, String sistemaPension, String afp) {
        return new TrabajadorRequest(" Ana Torres ", documento, "Analista", "Ventas", LocalDate.of(2025, 3, 1),
                new BigDecimal("2500.00"), LocalDate.of(1995, 6, 15), "987 654 321", "ana@empresa.pe", "",
                sistemaPension, afp, "BCP", "191-12345678-0-12");
    }

    private static Trabajador activo() {
        Trabajador t = new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", Trabajador.ACTIVO);
        t.setFechaIngreso(LocalDate.of(2025, 3, 1));
        return t;
    }

    @Test
    void registrarGuardaLaFichaCompletaComoActivo() {
        when(trabajadorRepository.existsByDocumentoIdentidad("70001234")).thenReturn(false);
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(inv -> {
            Trabajador t = inv.getArgument(0);
            assertNull(t.getId());
            t.setId(1L);
            return t;
        });

        TrabajadorResponse creado = personalService.registrar(ficha("70001234", "AFP", "Prima"));

        assertEquals(1L, creado.id());
        assertEquals(Trabajador.ACTIVO, creado.estado());
        assertEquals("Ana Torres", creado.nombres());
        assertEquals(new BigDecimal("2500.00"), creado.sueldo());
        assertEquals("Prima", creado.afp());
        assertNull(creado.direccion(), "los textos vacíos se guardan como null");
    }

    @Test
    void elNombreDeAfpSeDescartaSiElSistemaEsOnp() {
        when(trabajadorRepository.existsByDocumentoIdentidad("70001234")).thenReturn(false);
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(inv -> inv.getArgument(0));

        TrabajadorResponse creado = personalService.registrar(ficha("70001234", "ONP", "Prima"));

        assertEquals("ONP", creado.sistemaPension());
        assertNull(creado.afp());
    }

    @Test
    void registrarConDocumentoDuplicadoLanzaExcepcion() {
        when(trabajadorRepository.existsByDocumentoIdentidad("70001234")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> personalService.registrar(ficha("70001234", null, null)));
        verify(trabajadorRepository, never()).save(any());
    }

    @Test
    void actualizarNoCambiaElDocumento() {
        Trabajador existente = activo();
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(inv -> inv.getArgument(0));

        TrabajadorResponse actualizado = personalService.actualizar(1L, ficha("99999999", null, null));

        assertEquals("70001234", actualizado.documentoIdentidad());
        assertEquals(new BigDecimal("2500.00"), actualizado.sueldo());
    }

    @Test
    void cesarConservaElRegistroYGuardaFechaYMotivo() {
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(activo()));
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(inv -> inv.getArgument(0));

        TrabajadorResponse cesado = personalService.cesar(1L,
                new CesarTrabajadorRequest(LocalDate.of(2026, 10, 15), " Renuncia voluntaria "));

        assertEquals(Trabajador.CESADO, cesado.estado());
        assertEquals(LocalDate.of(2026, 10, 15), cesado.fechaCese());
        assertEquals("Renuncia voluntaria", cesado.motivoCese());
        verify(trabajadorRepository, never()).delete(any());
    }

    @Test
    void noSePuedeCesarAntesDeLaFechaDeIngreso() {
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(activo()));

        assertThrows(ReglaNegocioException.class, () -> personalService.cesar(1L,
                new CesarTrabajadorRequest(LocalDate.of(2024, 1, 1), "Fin de contrato")));
    }

    @Test
    void noSePuedeCesarDosVeces() {
        Trabajador cesado = activo();
        cesado.setEstado(Trabajador.CESADO);
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(cesado));

        assertThrows(ReglaNegocioException.class, () -> personalService.cesar(1L,
                new CesarTrabajadorRequest(LocalDate.of(2026, 10, 15), "Renuncia")));
    }

    @Test
    void reactivarLimpiaLosDatosDelCese() {
        Trabajador cesado = activo();
        cesado.setEstado(Trabajador.CESADO);
        cesado.setFechaCese(LocalDate.of(2026, 9, 30));
        cesado.setMotivoCese("Fin de contrato");
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(cesado));
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(inv -> inv.getArgument(0));

        TrabajadorResponse reactivado = personalService.reactivar(1L);

        assertEquals(Trabajador.ACTIVO, reactivado.estado());
        assertNull(reactivado.fechaCese());
        assertNull(reactivado.motivoCese());
    }

    @Test
    void eliminarConHistorialPideDarDeBaja() {
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(activo()));
        when(marcacionRepository.existsByTrabajadorId(1L)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> personalService.eliminar(1L));

        assertTrue(error.getMessage().contains("dalo de baja"));
        verify(trabajadorRepository, never()).delete(any());
    }

    @Test
    void eliminarSinHistorialBorraElRegistro() {
        Trabajador trabajador = activo();
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));

        personalService.eliminar(1L);

        verify(trabajadorRepository).delete(trabajador);
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(trabajadorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> personalService.obtener(999L));
    }

    @Test
    void listarFiltraPorAreaYEstado() {
        when(trabajadorRepository.findAll()).thenReturn(List.of(
                new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", Trabajador.ACTIVO),
                new Trabajador(2L, "Luis Ramos", "70005678", "Tecnico", "Almacen", Trabajador.CESADO),
                new Trabajador(3L, "Rosa Diaz", "70009999", "Tecnico", "Almacen", Trabajador.ACTIVO)));

        List<TrabajadorResponse> ventas = personalService.listar("Ventas", null);
        List<TrabajadorResponse> almacenActivos = personalService.listar("Almacen", "ACTIVO");

        assertEquals(1, ventas.size());
        assertEquals(1, almacenActivos.size());
        assertEquals("Rosa Diaz", almacenActivos.get(0).nombres());
    }

    @Test
    void buscarPorDocumentoUsaLaConsultaDelRepositorio() {
        when(trabajadorRepository.findByDocumentoIdentidad("70001234")).thenReturn(Optional.of(activo()));

        assertEquals("Ana Torres", personalService.buscarPorDocumento("70001234").orElseThrow().getNombres());
    }
}
