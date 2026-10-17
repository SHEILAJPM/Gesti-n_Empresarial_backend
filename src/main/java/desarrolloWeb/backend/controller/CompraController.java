package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.CompraRequest;
import desarrolloWeb.backend.dto.CompraResponse;
import desarrolloWeb.backend.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(
            @Valid @RequestBody CompraRequest request,
            @RequestParam(defaultValue = "false") boolean simularError) {
        CompraResponse creada = compraService.registrar(request, simularError);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<CompraResponse>> listar() {
        return ResponseEntity.ok(compraService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.buscarPorId(id));
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<CompraResponse>> listarPorProveedor(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(compraService.listarPorProveedor(proveedorId));
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<CompraResponse>> listarPorPeriodo(@RequestParam LocalDate desde,
                                                                 @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(compraService.listarPorPeriodo(desde, hasta));
    }

    @GetMapping("/igv-periodo")
    public ResponseEntity<BigDecimal> calcularIgvPeriodo(@RequestParam LocalDate desde,
                                                         @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(compraService.calcularTotalIgvPeriodo(desde, hasta));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompraResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody CompraRequest request) {
        return ResponseEntity.ok(compraService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        compraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
