package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ProveedorRequest;
import desarrolloWeb.backend.dto.ProveedorResponse;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProveedorService")
class ProveedorServiceTest {

    private static final String RUC = "20123456789";

    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    @Test
    @DisplayName("no se puede eliminar un proveedor con pagos registrados")
    void eliminarConPagos() {
        Proveedor proveedor = new Proveedor(RUC, "Distribuidora ACME S.A.C.", null);
        proveedor.setId(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(pagoRepository.existsByProveedor_Id(1L)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> proveedorService.eliminar(1L));

        assertEquals("No se puede eliminar el proveedor porque tiene pagos registrados", error.getMessage());
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("registrar guarda el proveedor y devuelve su id")
    void registrar() {
        when(proveedorRepository.existsByRuc(RUC)).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor proveedor = inv.getArgument(0);
            proveedor.setId(1L);
            return proveedor;
        });

        ProveedorResponse respuesta = proveedorService.registrar(
                new ProveedorRequest(RUC, "Distribuidora ACME S.A.C.", "Av. Arequipa 123"));

        assertAll(
                () -> assertEquals(1L, respuesta.id()),
                () -> assertEquals(RUC, respuesta.ruc()),
                () -> assertEquals("Av. Arequipa 123", respuesta.direccion())
        );
    }

    @Test
    @DisplayName("un RUC repetido lanza 409")
    void registrarDuplicado() {
        when(proveedorRepository.existsByRuc(RUC)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> proveedorService.registrar(new ProveedorRequest(RUC, "Otra S.A.C.", null)));

        assertEquals("Ya existe un proveedor registrado con el RUC " + RUC, error.getMessage());
    }

    @Test
    @DisplayName("un RUC con prefijo inválido lanza 400")
    void registrarRucInvalido() {
        DocumentoInvalidoException error = assertThrows(DocumentoInvalidoException.class,
                () -> proveedorService.registrar(new ProveedorRequest("30123456789", "Otra S.A.C.", null)));

        assertEquals("El RUC 30123456789 no es válido", error.getMessage());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorRuc conserva la firma Optional<Proveedor> que usa PagoService")
    void buscarPorRuc() {
        Proveedor proveedor = new Proveedor(RUC, "Distribuidora ACME S.A.C.", null);
        when(proveedorRepository.findByRuc(RUC)).thenReturn(Optional.of(proveedor));

        Optional<Proveedor> encontrado = proveedorService.buscarPorRuc(RUC);

        assertTrue(encontrado.isPresent());
        assertEquals("Distribuidora ACME S.A.C.", encontrado.get().getRazonSocial());
    }

    @Test
    @DisplayName("actualizar con el RUC de otro proveedor lanza 409")
    void actualizarConRucDeOtro() {
        Proveedor actual = new Proveedor("20987654321", "Actual S.A.C.", null);
        actual.setId(1L);
        Proveedor otro = new Proveedor(RUC, "Otro S.A.C.", null);
        otro.setId(2L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(proveedorRepository.findByRuc(RUC)).thenReturn(Optional.of(otro));

        assertThrows(ReglaNegocioException.class,
                () -> proveedorService.actualizar(1L, new ProveedorRequest(RUC, "Actual S.A.C.", null)));
    }

    @Test
    @DisplayName("actualizar modifica la entidad administrada sin llamar a save")
    void actualizarSinSave() {
        Proveedor actual = new Proveedor(RUC, "Actual S.A.C.", null);
        actual.setId(1L);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(proveedorRepository.findByRuc(RUC)).thenReturn(Optional.of(actual));

        ProveedorResponse respuesta = proveedorService.actualizar(1L,
                new ProveedorRequest(RUC, "Nuevo Nombre S.A.C.", "Jr. Lima 456"));

        assertEquals("Nuevo Nombre S.A.C.", respuesta.razonSocial());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar un proveedor inexistente lanza 404")
    void eliminarInexistente() {
        when(proveedorRepository.findById(9L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> proveedorService.eliminar(9L));

        assertEquals("No existe un proveedor con id 9", error.getMessage());
    }
}
