package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.UsuarioRequest;
import desarrolloWeb.backend.dto.UsuarioResponse;
import desarrolloWeb.backend.dto.VincularTrabajadorRequest;
import desarrolloWeb.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id,
                                                         @RequestParam boolean activo,
                                                         Authentication autenticacion) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, activo, autenticacion.getName()));
    }

    @PatchMapping("/{id}/trabajador")
    public ResponseEntity<UsuarioResponse> vincularTrabajador(@PathVariable Long id,
                                                              @RequestBody VincularTrabajadorRequest datos) {
        return ResponseEntity.ok(usuarioService.vincularTrabajador(id, datos.trabajadorId()));
    }
}
