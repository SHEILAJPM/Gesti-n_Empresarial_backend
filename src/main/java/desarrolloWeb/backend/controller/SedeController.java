package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.SedeRequest;
import desarrolloWeb.backend.dto.SedeResponse;
import desarrolloWeb.backend.service.SedeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* Ubicación de la empresa para la asistencia por GPS */
@RestController
@RequestMapping("/api/sede")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    /* 204 si todavía no se configuró */
    @GetMapping
    public ResponseEntity<SedeResponse> obtener() {
        return sedeService.obtener().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<SedeResponse> guardar(@Valid @RequestBody SedeRequest datos) {
        return ResponseEntity.ok(sedeService.guardar(datos));
    }
}
