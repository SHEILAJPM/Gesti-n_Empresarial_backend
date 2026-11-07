package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.CobranzaDtos.CobroRequest;
import desarrolloWeb.backend.dto.CobranzaDtos.CuentaPorCobrar;
import desarrolloWeb.backend.dto.CobranzaDtos.EstadoDeCuenta;
import desarrolloWeb.backend.dto.CobranzaDtos.Resumen;
import desarrolloWeb.backend.service.CobranzaService;
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

/* Cuentas por cobrar de las ventas al crédito */
@RestController
@RequestMapping("/api/cobranzas")
public class CobranzaController {

    private final CobranzaService cobranzaService;

    public CobranzaController(CobranzaService cobranzaService) {
        this.cobranzaService = cobranzaService;
    }

    /* Facturas al crédito con saldo; ?todas=true incluye las ya pagadas */
    @GetMapping
    public ResponseEntity<List<CuentaPorCobrar>> listar(@RequestParam(defaultValue = "false") boolean todas) {
        return ResponseEntity.ok(cobranzaService.listar(todas));
    }

    @GetMapping("/resumen")
    public ResponseEntity<Resumen> resumen() {
        return ResponseEntity.ok(cobranzaService.resumen());
    }

    /* Cuotas de una venta con lo cobrado y la lista de cobros */
    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<EstadoDeCuenta> estadoDeCuenta(@PathVariable Long ventaId) {
        return ResponseEntity.ok(cobranzaService.estadoDeCuenta(ventaId));
    }

    @PostMapping
    public ResponseEntity<EstadoDeCuenta> registrar(@Valid @RequestBody CobroRequest datos, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cobranzaService.registrarCobro(datos, autenticacion.getName()));
    }
}
