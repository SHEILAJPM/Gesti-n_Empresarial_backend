package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ClienteRequest;
import desarrolloWeb.backend.dto.ClienteResponse;
import desarrolloWeb.backend.entity.Cliente;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.ClienteRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.ValidadorDocumento;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final ValidadorDocumento validador = new ValidadorDocumento();

    public ClienteService(ClienteRepository clienteRepository,
                          VentaRepository ventaRepository) {
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION')")
    @Transactional
    public ClienteResponse registrar(ClienteRequest request) {
        TipoDocumento tipoDocumento = validador.tipoDocumentoDe(request.documento());

        if (clienteRepository.existsByDocumento(request.documento())) {
            throw new ReglaNegocioException("Ya existe un cliente registrado con el documento " + request.documento());
        }

        Cliente cliente = toEntity(request, tipoDocumento);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return mapear(clienteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String documento) {
        return clienteRepository.findByDocumento(documento)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un cliente con documento " + documento));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorTexto(String texto) {
        return mapear(clienteRepository.buscarPorTexto(texto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION')")
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtenerEntidad(id);
        TipoDocumento tipoDocumento = validador.tipoDocumentoDe(request.documento());

        clienteRepository.findByDocumento(request.documento())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException(
                            "Ya existe un cliente registrado con el documento " + request.documento());
                });

        copiarDatos(request, tipoDocumento, cliente);
        return toResponse(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION')")
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtenerEntidad(id);
        if (ventaRepository.contarPorCliente(id) > 0) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el cliente " + cliente.getDocumento() + " porque tiene ventas registradas");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente obtenerEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un cliente con id " + id));
    }

    private List<ClienteResponse> mapear(List<Cliente> clientes) {
        List<ClienteResponse> respuesta = new ArrayList<>();
        for (Cliente cliente : clientes) {
            respuesta.add(toResponse(cliente));
        }
        return respuesta;
    }

    private Cliente toEntity(ClienteRequest request, TipoDocumento tipoDocumento) {
        return new Cliente(request.documento(), request.nombre(), tipoDocumento);
    }

    private void copiarDatos(ClienteRequest request, TipoDocumento tipoDocumento, Cliente cliente) {
        cliente.setDocumento(request.documento());
        cliente.setNombre(request.nombre());
        cliente.setTipoDocumento(tipoDocumento);
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getDocumento(),
                cliente.getNombre(),
                cliente.getTipoDocumento());
    }
}
