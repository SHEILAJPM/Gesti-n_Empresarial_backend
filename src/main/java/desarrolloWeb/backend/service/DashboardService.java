package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.DashboardMetricas;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final PersonalService personalService;
    private final AsistenciaService asistenciaService;
    private final PagoService pagoService;
    private final DocumentoService documentoService;
    private final CompraService compraService;
    private final VentaService ventaService;

    public DashboardService(
            PersonalService personalService,
            AsistenciaService asistenciaService,
            PagoService pagoService,
            DocumentoService documentoService,
            CompraService compraService,
            VentaService ventaService) {
        this.personalService = personalService;
        this.asistenciaService = asistenciaService;
        this.pagoService = pagoService;
        this.documentoService = documentoService;
        this.compraService = compraService;
        this.ventaService = ventaService;
    }

    public DashboardMetricas obtenerMetricas() {
        return new DashboardMetricas(
                personalService.listar(null, null).size(),
                personalService.listar(null, "ACTIVO").size(),
                asistenciaService.listarTodas().size(),
                pagoService.listarPendientes().size(),
                documentoService.listarProximosAVencer(30).size(),
                documentoService.listarVencidos().size(),
                compraService.listarTodas().size(),
                ventaService.listarTodas().size());
    }
}
