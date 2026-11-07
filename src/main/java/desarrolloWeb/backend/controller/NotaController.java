package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.NotaRequest;
import desarrolloWeb.backend.dto.NotaResponse;
import desarrolloWeb.backend.entity.TipoNota;
import desarrolloWeb.backend.service.NotaService;
import desarrolloWeb.backend.util.MotivoNota;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/* Notas de crédito y débito */
@RestController
@RequestMapping("/api/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    /* Todas, o solo las de una venta con ?ventaId= */
    @GetMapping
    public ResponseEntity<List<NotaResponse>> listar(@RequestParam(required = false) Long ventaId) {
        return ResponseEntity.ok(ventaId == null ? notaService.listar() : notaService.listarPorVenta(ventaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(notaService.obtener(id));
    }

    /* Motivos permitidos (catálogos 09 y 10 de SUNAT) */
    @GetMapping("/motivos")
    public ResponseEntity<Map<TipoNota, Map<String, String>>> motivos() {
        return ResponseEntity.ok(Map.of(TipoNota.CREDITO, MotivoNota.CREDITO, TipoNota.DEBITO, MotivoNota.DEBITO));
    }

    @PostMapping
    public ResponseEntity<NotaResponse> emitir(@Valid @RequestBody NotaRequest datos, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaService.emitir(datos, autenticacion.getName()));
    }

    @GetMapping("/{id}/sunat")
    public ResponseEntity<NotaResponse> consultarSunat(@PathVariable Long id) {
        return ResponseEntity.ok(notaService.consultarSunat(id));
    }

    @PostMapping("/{id}/sunat/reenviar")
    public ResponseEntity<NotaResponse> reenviarSunat(@PathVariable Long id) {
        return ResponseEntity.ok(notaService.reenviarSunat(id));
    }
}
