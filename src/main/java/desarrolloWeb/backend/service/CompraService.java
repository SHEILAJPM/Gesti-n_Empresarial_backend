package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.CompraRequest;
import desarrolloWeb.backend.dto.CompraResponse;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.CompraRepository;
import desarrolloWeb.backend.util.CalculadoraTributaria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorService proveedorService;
    private final PagoService pagoService;
    private final ConfiguracionTributariaService configuracionService;
    private final CalculadoraTributaria calculadora = new CalculadoraTributaria();

    public CompraService(CompraRepository compraRepository,
                         ProveedorService proveedorService,
                         PagoService pagoService,
                         ConfiguracionTributariaService configuracionService) {
        this.compraRepository = compraRepository;
        this.proveedorService = proveedorService;
        this.pagoService = pagoService;
        this.configuracionService = configuracionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public CompraResponse registrar(CompraRequest request, boolean simularError) {
        Proveedor proveedor = proveedorService.buscarPorRuc(request.ruc())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un proveedor con RUC " + request.ruc()));

        if (compraRepository.findByProveedorIdAndNumeroComprobante(
                proveedor.getId(), request.numeroComprobante()).isPresent()) {
            throw new ReglaNegocioException("El proveedor " + request.ruc()
                    + " ya tiene registrada la compra " + request.numeroComprobante());
        }

        BigDecimal tasa = configuracionService.obtenerTasaVigente(ConfiguracionTributariaService.CODIGO_IGV);
        BigDecimal base = request.montoSinIgv().setScale(2, RoundingMode.HALF_UP);

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setNumeroComprobante(request.numeroComprobante());
        compra.setFecha(request.fecha());
        compra.setMontoSinIgv(base);
        compra.setIgv(calculadora.calcularIgv(base, tasa));
        compra.setMontoTotal(calculadora.calcularTotal(base, tasa));
        compra.setTasaAplicada(tasa);

        Compra guardada = compraRepository.saveAndFlush(compra);

        if (simularError) {
            throw new RuntimeException("ERROR SIMULADO: la compra " + request.numeroComprobante()
                    + " se insertó, pero la transacción hará ROLLBACK");
        }

        pagoService.registrarProveedor(request.ruc(), "Compra " + request.numeroComprobante(),
                guardada.getMontoTotal(), request.fecha());

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listarTodas() {
        return mapear(compraRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listarPorProveedor(Long proveedorId) {
        return mapear(compraRepository.buscarPorProveedor(proveedorId));
    }

    @Transactional(readOnly = true)
    public CompraResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public CompraResponse actualizar(Long id, CompraRequest request) {
        Compra compra = obtenerEntidad(id);
        Proveedor proveedor = proveedorService.buscarPorRuc(request.ruc())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un proveedor con RUC " + request.ruc()));

        compraRepository.findByProveedorIdAndNumeroComprobante(proveedor.getId(), request.numeroComprobante())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException("El proveedor " + request.ruc()
                            + " ya tiene registrada la compra " + request.numeroComprobante());
                });

        BigDecimal tasa = compra.getTasaAplicada();
        BigDecimal base = request.montoSinIgv().setScale(2, RoundingMode.HALF_UP);

        compra.setProveedor(proveedor);
        compra.setNumeroComprobante(request.numeroComprobante());
        compra.setFecha(request.fecha());
        compra.setMontoSinIgv(base);
        compra.setIgv(calculadora.calcularIgv(base, tasa));
        compra.setMontoTotal(calculadora.calcularTotal(base, tasa));

        return toResponse(compra);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRACION', 'CONTABILIDAD')")
    @Transactional
    public void eliminar(Long id) {
        compraRepository.delete(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return mapear(compraRepository.buscarPorPeriodo(desde, hasta));
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalIgvPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Compra compra : compraRepository.buscarPorPeriodo(desde, hasta)) {
            total = total.add(compra.getIgv());
        }
        return total;
    }

    private Compra obtenerEntidad(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una compra con id " + id));
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private List<CompraResponse> mapear(List<Compra> compras) {
        List<CompraResponse> respuesta = new ArrayList<>();
        for (Compra compra : compras) {
            respuesta.add(toResponse(compra));
        }
        return respuesta;
    }

    private CompraResponse toResponse(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getProveedor().getId(),
                compra.getProveedor().getRuc(),
                compra.getProveedor().getRazonSocial(),
                compra.getNumeroComprobante(),
                compra.getFecha(),
                compra.getMontoSinIgv(),
                compra.getIgv(),
                compra.getMontoTotal(),
                compra.getTasaAplicada());
    }
}
