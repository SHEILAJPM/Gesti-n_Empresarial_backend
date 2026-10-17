package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.AprobarPlanillaRequest;
import desarrolloWeb.backend.dto.GenerarPlanillaRequest;
import desarrolloWeb.backend.dto.PlanillaResponse;
import desarrolloWeb.backend.service.PlanillaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planillas")
public class PlanillaController {

    private final PlanillaService planillaService;

    public PlanillaController(PlanillaService planillaService) {
        this.planillaService = planillaService;
    }

    @GetMapping
    public ResponseEntity<List<PlanillaResponse>> listar() {
        return ResponseEntity.ok(planillaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanillaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(planillaService.obtener(id));
    }

    /* Genera (o regenera si sigue en borrador) la planilla del mes */
    @PostMapping("/generar")
    public ResponseEntity<PlanillaResponse> generar(@Valid @RequestBody GenerarPlanillaRequest datos,
                                                    Authentication autenticacion) {
        return ResponseEntity.ok(planillaService.generar(datos.anio(), datos.mes(), autenticacion.getName()));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<PlanillaResponse> aprobar(@PathVariable Long id,
                                                    @Valid @RequestBody AprobarPlanillaRequest datos,
                                                    Authentication autenticacion) {
        return ResponseEntity.ok(planillaService.aprobar(id, datos.fechaPago(), autenticacion.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        planillaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
