package desarrolloWeb.backend.controller;

import desarrolloWeb.backend.dto.DocumentoConsultado;
import desarrolloWeb.backend.service.ConsultaDocumentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ConsultaDocumentoService consultaService;

    public ConsultaController(ConsultaDocumentoService consultaService) {
        this.consultaService = consultaService;
    }

    /* DNI (8 dígitos) -> nombre en RENIEC; RUC (11 dígitos) -> razón social y dirección en SUNAT */
    @GetMapping("/documento/{numero}")
    public ResponseEntity<DocumentoConsultado> consultar(@PathVariable String numero) {
        return ResponseEntity.ok(consultaService.consultar(numero));
    }
}
