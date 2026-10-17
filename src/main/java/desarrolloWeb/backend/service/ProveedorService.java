package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ProveedorRequest;
import desarrolloWeb.backend.dto.ProveedorResponse;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.PagoRepository;
import desarrolloWeb.backend.repository.ProveedorRepository;
import desarrolloWeb.backend.util.ValidadorDocumento;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final PagoRepository pagoRepository;
    private final ValidadorDocumento validador = new ValidadorDocumento();

    public ProveedorService(ProveedorRepository proveedorRepository, PagoRepository pagoRepository) {
        this.proveedorRepository = proveedorRepository;
        this.pagoRepository = pagoRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public ProveedorResponse registrar(ProveedorRequest request) {
        validarRuc(request.ruc());
        if (proveedorRepository.existsByRuc(request.ruc())) {
            throw new ReglaNegocioException("Ya existe un proveedor registrado con el RUC " + request.ruc());
        }

        Proveedor proveedor = toEntity(request);
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarTodos() {
        List<ProveedorResponse> respuesta = new ArrayList<>();
        for (Proveedor proveedor : proveedorRepository.findAll()) {
            respuesta.add(toResponse(proveedor));
        }
        return respuesta;
    }

    @Transactional(readOnly = true)
    public ProveedorResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Optional<Proveedor> buscarPorRuc(String ruc) {
        return proveedorRepository.findByRuc(ruc);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = obtenerEntidad(id);
        validarRuc(request.ruc());

        proveedorRepository.findByRuc(request.ruc())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un proveedor registrado con el RUC " + request.ruc());
                });

        copiarDatos(request, proveedor);
        return toResponse(proveedor);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = obtenerEntidad(id);
        if (pagoRepository.existsByProveedor_Id(id)) {
            throw new ReglaNegocioException("No se puede eliminar el proveedor porque tiene pagos registrados");
        }
        proveedorRepository.delete(proveedor);
        proveedorRepository.flush();
    }

    private Proveedor obtenerEntidad(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un proveedor con id " + id));
    }

    private void validarRuc(String ruc) {
        if (!validador.esRuc(ruc)) {
            throw new DocumentoInvalidoException("El RUC " + ruc + " no es válido");
        }
    }

    private Proveedor toEntity(ProveedorRequest request) {
        return new Proveedor(request.ruc(), request.razonSocial(), request.direccion());
    }

    private void copiarDatos(ProveedorRequest request, Proveedor proveedor) {
        proveedor.setRuc(request.ruc());
        proveedor.setRazonSocial(request.razonSocial());
        proveedor.setDireccion(request.direccion());
    }

    private ProveedorResponse toResponse(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getRuc(),
                proveedor.getRazonSocial(),
                proveedor.getDireccion());
    }
}
