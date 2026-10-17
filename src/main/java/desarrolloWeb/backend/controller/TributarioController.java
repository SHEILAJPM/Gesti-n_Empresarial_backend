package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.ActualizarTasaRequest;
import desarrolloWeb.backend.dto.ConfiguracionResponse;
import desarrolloWeb.backend.dto.ResumenTributario;
import desarrolloWeb.backend.dto.SimulacionResponse;
import desarrolloWeb.backend.service.ConfiguracionTributariaService;
import desarrolloWeb.backend.service.TributarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tributario")
public class TributarioController {

    private final TributarioService tributarioService;
    private final ConfiguracionTributariaService configuracionService;

    public TributarioController(TributarioService tributarioService,
                                ConfiguracionTributariaService configuracionService) {
        this.tributarioService = tributarioService;
        this.configuracionService = configuracionService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenTributario> resumen(@RequestParam LocalDate desde,
                                                     @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(tributarioService.generarResumen(desde, hasta));
    }

    @GetMapping("/resumen-mensual")
    public ResponseEntity<ResumenTributario> resumenMensual(@RequestParam int anio,
                                                            @RequestParam int mes) {
        return ResponseEntity.ok(tributarioService.generarResumenMensual(anio, mes));
    }

    @GetMapping("/configuracion")
    public ResponseEntity<List<ConfiguracionResponse>> listarConfiguracion() {
        return ResponseEntity.ok(configuracionService.listar());
    }

    @PutMapping("/configuracion/{codigo}")
    public ResponseEntity<ConfiguracionResponse> actualizarTasa(@PathVariable String codigo,
                                                                @Valid @RequestBody ActualizarTasaRequest request) {
        return ResponseEntity.ok(configuracionService.actualizarTasa(codigo, request));
    }

    @GetMapping("/calcular")
    public ResponseEntity<SimulacionResponse> calcular(@RequestParam BigDecimal monto,
                                                       @RequestParam(defaultValue = "false") boolean incluyeIgv) {
        return ResponseEntity.ok(tributarioService.simular(monto, incluyeIgv));
    }
}
