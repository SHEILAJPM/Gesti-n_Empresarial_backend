package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.InventarioDtos.CompraParaIngreso;
import desarrolloWeb.backend.dto.InventarioDtos.Kardex;
import desarrolloWeb.backend.dto.InventarioDtos.Motivos;
import desarrolloWeb.backend.dto.InventarioDtos.Movimiento;
import desarrolloWeb.backend.dto.InventarioDtos.MovimientoRequest;
import desarrolloWeb.backend.dto.InventarioDtos.ProductoStock;
import desarrolloWeb.backend.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /* Stock de cada producto (los servicios no aparecen) */
    @GetMapping
    public ResponseEntity<List<ProductoStock>> listar() {
        return ResponseEntity.ok(inventarioService.listar());
    }

    @GetMapping("/{productoId}/kardex")
    public ResponseEntity<Kardex> kardex(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.kardex(productoId));
    }

    @GetMapping("/motivos")
    public ResponseEntity<Motivos> motivos() {
        return ResponseEntity.ok(inventarioService.motivos());
    }

    /* Compras recientes para vincular un ingreso al almacén */
    @GetMapping("/compras")
    public ResponseEntity<List<CompraParaIngreso>> compras() {
        return ResponseEntity.ok(inventarioService.comprasRecientes());
    }

    @PostMapping("/movimientos")
    public ResponseEntity<List<Movimiento>> registrar(@Valid @RequestBody MovimientoRequest datos,
                                                      Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrar(datos, autenticacion.getName()));
    }
}
