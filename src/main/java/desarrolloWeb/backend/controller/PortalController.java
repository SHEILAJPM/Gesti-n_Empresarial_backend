package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.MarcarUbicacionRequest;
import desarrolloWeb.backend.dto.MiBoletaResponse;
import desarrolloWeb.backend.dto.MiPerfilResponse;
import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.service.PortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* "Mi espacio": todo se resuelve con el usuario autenticado */
@RestController
@RequestMapping("/api/mi")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<MiPerfilResponse> perfil(Authentication autenticacion) {
        return ResponseEntity.ok(portalService.perfil(autenticacion.getName()));
    }

    @PostMapping("/asistencia")
    public ResponseEntity<Marcacion> marcar(@Valid @RequestBody MarcarUbicacionRequest ubicacion, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.marcar(autenticacion.getName(), ubicacion));
    }

    @GetMapping("/asistencia")
    public ResponseEntity<List<Marcacion>> misMarcaciones(Authentication autenticacion) {
        return ResponseEntity.ok(portalService.misMarcaciones(autenticacion.getName()));
    }

    @GetMapping("/boletas")
    public ResponseEntity<List<MiBoletaResponse>> misBoletas(Authentication autenticacion) {
        return ResponseEntity.ok(portalService.misBoletas(autenticacion.getName()));
    }
}
