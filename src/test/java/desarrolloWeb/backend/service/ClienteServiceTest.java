package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ClienteRequest;
import desarrolloWeb.backend.dto.ClienteResponse;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.VentaRepository;
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
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("registrar con 8 dígitos deduce tipoDocumento DNI")
    void registrarDeduceDni() {
        when(clienteRepository.existsByDocumento("10203040")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente cliente = inv.getArgument(0);
            cliente.setId(1L);
            return cliente;
        });

        ClienteResponse respuesta = clienteService.registrar(new ClienteRequest("10203040", "Carlos Mendoza"));

        assertAll(
                () -> assertEquals(1L, respuesta.id()),
                () -> assertEquals(TipoDocumento.DNI, respuesta.tipoDocumento())
        );
    }

    @Test
    @DisplayName("registrar con un RUC válido deduce tipoDocumento RUC")
    void registrarDeduceRuc() {
        when(clienteRepository.existsByDocumento("20555666777")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse respuesta = clienteService.registrar(
                new ClienteRequest("20555666777", "Comercial Lima S.A.C."));

        assertEquals(TipoDocumento.RUC, respuesta.tipoDocumento());
    }

    @Test
    @DisplayName("un RUC con prefijo inválido lanza DocumentoInvalidoException")
    void registrarRucInvalido() {
        assertThrows(DocumentoInvalidoException.class,
                () -> clienteService.registrar(new ClienteRequest("30555666777", "Empresa X")));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("un documento repetido lanza 409")
    void registrarDuplicado() {
        when(clienteRepository.existsByDocumento("10203040")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrar(new ClienteRequest("10203040", "Otro Cliente")));

        assertEquals("Ya existe un cliente registrado con el documento 10203040", error.getMessage());
    }

    @Test
    @DisplayName("actualizar modifica la entidad administrada sin llamar a save")
    void actualizarSinSave() {
        Cliente existente = new Cliente("10203040", "Carlos", TipoDocumento.DNI);
        existente.setId(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByDocumento("10203040")).thenReturn(Optional.of(existente));

        ClienteResponse respuesta = clienteService.actualizar(1L, new ClienteRequest("10203040", "Carlos Mendoza"));

        assertEquals("Carlos Mendoza", respuesta.nombre());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se puede eliminar un cliente con ventas registradas")
    void eliminarConVentas() {
        Cliente existente = new Cliente("10203040", "Carlos", TipoDocumento.DNI);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepository.contarPorCliente(1L)).thenReturn(2L);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> clienteService.eliminar(1L));

        assertEquals("No se puede eliminar el cliente 10203040 porque tiene ventas registradas", error.getMessage());
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminar un cliente sin ventas lo borra")
    void eliminarSinVentas() {
        Cliente existente = new Cliente("10203040", "Carlos", TipoDocumento.DNI);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepository.contarPorCliente(1L)).thenReturn(0L);

        clienteService.eliminar(1L);

        verify(clienteRepository).delete(existente);
    }

    @Test
    @DisplayName("buscarPorId inexistente lanza 404")
    void buscarPorIdInexistente() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> clienteService.buscarPorId(9L));

        assertEquals("No existe un cliente con id 9", error.getMessage());
    }
}
