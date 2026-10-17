package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Pago;
import desarrolloWeb.backend.entity.Proveedor;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PagoService {

    private final PersonalService personalService;
    private final ProveedorService proveedorService;
    private final PagoRepository pagoRepository;
    private final List<Pago> pagos = new ArrayList<>();
    private final AtomicLong contadorId = new AtomicLong(0);

    @Autowired
    public PagoService(PersonalService personalService, ProveedorService proveedorService,
                       PagoRepository pagoRepository) {
        this.personalService = personalService;
        this.proveedorService = proveedorService;
        this.pagoRepository = pagoRepository;
    }

    /** Constructor para uso aislado del servicio, por ejemplo en pruebas unitarias. */
    public PagoService(PersonalService personalService, ProveedorService proveedorService) {
        this.personalService = personalService;
        this.proveedorService = proveedorService;
        this.pagoRepository = null;
    }

    @Transactional
    public Pago registrar(String documentoIdentidad, String concepto, Double monto, LocalDate fechaProgramada) {
        validarDatos(concepto, monto, fechaProgramada);
        Trabajador trabajador = personalService.buscarPorDocumento(documentoIdentidad)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe un trabajador con documento " + documentoIdentidad));

        Pago pago = new Pago();
        pago.setTipoBeneficiario("TRABAJADOR");
        pago.setTrabajador(trabajador);
        pago.setConcepto(concepto);
        pago.setMonto(monto);
        pago.setFechaProgramada(fechaProgramada);
        pago.setEstado("PENDIENTE");

        return guardar(pago);
    }

    @Transactional
    public Pago registrarProveedor(String ruc, String concepto, Double monto, LocalDate fechaProgramada) {
        validarDatos(concepto, monto, fechaProgramada);
        Proveedor proveedor = proveedorService.buscarPorRuc(ruc)
                .orElseThrow(() -> new NoSuchElementException("No existe un proveedor con RUC " + ruc));

        Pago pago = new Pago();
        pago.setTipoBeneficiario("PROVEEDOR");
        pago.setProveedor(proveedor);
        pago.setConcepto(concepto);
        pago.setMonto(monto);
        pago.setFechaProgramada(fechaProgramada);
        pago.setEstado("PENDIENTE");

        return guardar(pago);
    }

    private Pago guardar(Pago pago) {
        if (pagoRepository != null) {
            return pagoRepository.save(pago);
        }
        pago.setId(contadorId.incrementAndGet());
        pagos.add(pago);
        return pago;
    }

    public Pago registrar(String tipoBeneficiario, String documentoIdentidad, String rucProveedor,
                          String concepto, Double monto, LocalDate fechaProgramada) {
        if (tipoBeneficiario == null || tipoBeneficiario.isBlank()) {
            tipoBeneficiario = rucProveedor != null && !rucProveedor.isBlank()
                    ? "PROVEEDOR" : "TRABAJADOR";
        }

        return switch (tipoBeneficiario.trim().toUpperCase()) {
            case "TRABAJADOR" -> registrar(documentoIdentidad, concepto, monto, fechaProgramada);
            case "PROVEEDOR" -> registrarProveedor(rucProveedor, concepto, monto, fechaProgramada);
            default -> throw new IllegalArgumentException(
                    "El tipo de beneficiario debe ser TRABAJADOR o PROVEEDOR");
        };
    }

    private void validarDatos(String concepto, Double monto, LocalDate fechaProgramada) {
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto del pago es obligatorio");
        }
        if (monto == null || !Double.isFinite(monto) || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        if (fechaProgramada == null) {
            throw new IllegalArgumentException("La fecha programada es obligatoria");
        }
    }

    @Transactional
    public Pago marcarComoPagado(Long id) {
        Pago pago = buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe un pago con id " + id));

        if ("PAGADO".equals(pago.getEstado())) {
            throw new IllegalStateException("El pago " + id + " ya estaba marcado como pagado");
        }

        pago.setEstado("PAGADO");
        pago.setFechaPago(LocalDateTime.now());
        return pagoRepository == null ? pago : pagoRepository.save(pago);
    }

    public Optional<Pago> buscarPorId(Long id) {
        if (pagoRepository != null) {
            return pagoRepository.findById(id);
        }
        return pagos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public List<Pago> listarTodos() {
        if (pagoRepository != null) {
            return pagoRepository.findAll();
        }
        return List.copyOf(pagos);
    }

    public List<Pago> listarPendientes() {
        if (pagoRepository != null) {
            return pagoRepository.findByEstado("PENDIENTE");
        }
        return pagos.stream()
                .filter(p -> "PENDIENTE".equals(p.getEstado()))
                .toList();
    }

    public List<Pago> listarPorTrabajador(Long trabajadorId) {
        if (pagoRepository != null) {
            return pagoRepository.findByTrabajador_Id(trabajadorId);
        }
        return pagos.stream()
                .filter(p -> trabajadorId.equals(p.getTrabajadorId()))
                .toList();
    }

    public List<Pago> listarPorProveedor(Long proveedorId) {
        if (pagoRepository != null) {
            return pagoRepository.findByProveedor_Id(proveedorId);
        }
        return pagos.stream()
                .filter(p -> proveedorId.equals(p.getProveedorId()))
                .toList();
    }
}
