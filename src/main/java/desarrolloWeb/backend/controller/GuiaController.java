package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.GuiaRequest;
import desarrolloWeb.backend.dto.GuiaResponse;
import desarrolloWeb.backend.service.GuiaService;
import desarrolloWeb.backend.util.MotivoTraslado;
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

/* Guías de remisión remitente */
@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    /* Todas, o solo las de una venta con ?ventaId= */
    @GetMapping
    public ResponseEntity<List<GuiaResponse>> listar(@RequestParam(required = false) Long ventaId) {
        return ResponseEntity.ok(ventaId == null ? guiaService.listar() : guiaService.listarPorVenta(ventaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.obtener(id));
    }

    /* Motivos de traslado (catálogo 20 de SUNAT) */
    @GetMapping("/motivos")
    public ResponseEntity<Map<String, String>> motivos() {
        return ResponseEntity.ok(MotivoTraslado.CATALOGO);
    }

    @PostMapping
    public ResponseEntity<GuiaResponse> emitir(@Valid @RequestBody GuiaRequest datos, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.emitir(datos, autenticacion.getName()));
    }

    @GetMapping("/{id}/sunat")
    public ResponseEntity<GuiaResponse> consultarSunat(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.consultarSunat(id));
    }

    @PostMapping("/{id}/sunat/reenviar")
    public ResponseEntity<GuiaResponse> reenviarSunat(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.reenviarSunat(id));
    }
}
