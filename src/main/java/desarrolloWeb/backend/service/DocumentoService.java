
package desarrolloWeb.backend.service;

import desarrolloWeb.backend.entity.Documento;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.repository.DocumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DocumentoService {

    private final PersonalService personalService;
    private final DocumentoRepository documentoRepository;

    public DocumentoService(
            PersonalService personalService,
            DocumentoRepository documentoRepository) {
        this.personalService = personalService;
        this.documentoRepository = documentoRepository;
    }

    @Transactional
    public Documento registrar(
            String documentoIdentidadTrabajador,
            String tipo,
            LocalDate fechaVencimiento) {

        Trabajador trabajador =
                personalService.buscarPorDocumento(
                        documentoIdentidadTrabajador
                ).orElseThrow(() -> new NoSuchElementException(
                        "No existe un trabajador con documento "
                                + documentoIdentidadTrabajador));

        Documento documento = new Documento();
        documento.setTrabajadorId(trabajador.getId());
        documento.setTipo(tipo);
        documento.setFechaVencimiento(fechaVencimiento);

        return documentoRepository.save(documento);
    }

    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    public List<Documento> listarPorTrabajador(Long trabajadorId) {
        return documentoRepository.findByTrabajadorId(trabajadorId);
    }

    public List<Documento> listarPorTrabajadorJPQL(Long trabajadorId) {
        return documentoRepository.buscarPorTrabajadorJPQL(trabajadorId);
    }

    public List<Documento> listarProximosAVencer(int diasUmbral) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(Math.max(0, diasUmbral));

        return documentoRepository.findAll().stream()
                .filter(d -> d.getFechaVencimiento() != null)
                .filter(d -> !d.getFechaVencimiento().isBefore(hoy))
                .filter(d -> !d.getFechaVencimiento().isAfter(limite))
                .toList();
    }

    public List<Documento> listarVencidos() {
        LocalDate hoy = LocalDate.now();

        return documentoRepository.findAll().stream()
                .filter(d -> d.getFechaVencimiento() != null)
                .filter(d -> d.getFechaVencimiento().isBefore(hoy))
                .toList();
    }
}
