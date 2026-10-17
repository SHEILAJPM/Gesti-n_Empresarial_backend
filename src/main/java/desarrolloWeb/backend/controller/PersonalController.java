package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.CesarTrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorRequest;
import desarrolloWeb.backend.dto.TrabajadorResponse;
import desarrolloWeb.backend.service.PersonalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/personal")
public class PersonalController {

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public ResponseEntity<List<TrabajadorResponse>> listar(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(personalService.listar(area, estado));
    }

    @GetMapping("/area/{area}")
    public ResponseEntity<List<TrabajadorResponse>> listarPorArea(@PathVariable String area) {
        return ResponseEntity.ok(personalService.listarPorArea(area));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrabajadorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(personalService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TrabajadorResponse> registrar(@Valid @RequestBody TrabajadorRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personalService.registrar(datos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrabajadorResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody TrabajadorRequest datos) {
        return ResponseEntity.ok(personalService.actualizar(id, datos));
    }

    @PatchMapping("/{id}/cesar")
    public ResponseEntity<TrabajadorResponse> cesar(@PathVariable Long id,
                                                    @Valid @RequestBody CesarTrabajadorRequest datos) {
        return ResponseEntity.ok(personalService.cesar(id, datos));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<TrabajadorResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(personalService.reactivar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personalService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
