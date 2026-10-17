package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Documento;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.DocumentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private PersonalService personalService;
    @Mock
    private DocumentoRepository documentoRepository;

    @InjectMocks
    private DocumentoService documentoService;

    @Test
    void registrarConDocumentoInexistenteLanzaExcepcion() {
        when(personalService.buscarPorDocumento("99999999")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> documentoService.registrar("99999999", "DNI", LocalDate.now().plusDays(10)));
    }

    @Test
    void registrarQuedaLigadoAlTrabajador() {
        Trabajador trabajador = new Trabajador(1L, "Ana Torres", "70001234", "Analista", "Ventas", "ACTIVO");
        when(personalService.buscarPorDocumento("70001234")).thenReturn(Optional.of(trabajador));
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        Documento documento = documentoService.registrar("70001234", "Licencia de conducir",
                LocalDate.now().plusDays(10));

        assertNotNull(documento.getId());
        assertEquals(trabajador.getId(), documento.getTrabajadorId());
        assertEquals("Licencia de conducir", documento.getTipo());
    }

    @Test
    void listarProximosAVencerIncluyeSoloLosDentroDelRango() {
        Documento porVencer = documento(1L, LocalDate.now().plusDays(5));
        when(documentoRepository.findAll()).thenReturn(List.of(
                porVencer, documento(2L, LocalDate.now().plusDays(90)), documento(3L, LocalDate.now().minusDays(1))));

        List<Documento> proximos = documentoService.listarProximosAVencer(30);

        assertEquals(1, proximos.size());
        assertEquals(porVencer.getId(), proximos.get(0).getId());
    }

    @Test
    void listarVencidosIncluyeSoloFechasPasadas() {
        Documento vencido = documento(1L, LocalDate.now().minusDays(1));
        when(documentoRepository.findAll()).thenReturn(List.of(vencido, documento(2L, LocalDate.now().plusDays(10))));

        List<Documento> vencidos = documentoService.listarVencidos();

        assertEquals(1, vencidos.size());
        assertEquals(vencido.getId(), vencidos.get(0).getId());
    }

    @Test
    void listarPorTrabajadorDevuelveSoloSusDocumentos() {
        when(documentoRepository.findByTrabajadorId(1L)).thenReturn(List.of(new Documento(), new Documento()));

        assertEquals(2, documentoService.listarPorTrabajador(1L).size());
    }

    private Documento documento(Long id, LocalDate vencimiento) {
        Documento documento = new Documento();
        documento.setId(id);
        documento.setTrabajadorId(1L);
        documento.setTipo("DNI");
        documento.setFechaVencimiento(vencimiento);
        return documento;
    }
}
