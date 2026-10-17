package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.ActualizarTasaRequest;
import desarrolloWeb.backend.dto.ConfiguracionResponse;
import desarrolloWeb.backend.entity.ConfiguracionTributaria;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.repository.ConfiguracionTributariaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfiguracionTributariaService {

    public static final String CODIGO_IGV = "IGV";

    private final ConfiguracionTributariaRepository configuracionRepository;

    public ConfiguracionTributariaService(ConfiguracionTributariaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerTasaVigente(String codigo) {
        return configuracionRepository.buscarVigente(codigo)
                .map(ConfiguracionTributaria::getTasa)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una tasa vigente para " + codigo));
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionResponse> listar() {
        List<ConfiguracionResponse> respuesta = new ArrayList<>();
        for (ConfiguracionTributaria configuracion : configuracionRepository.listarOrdenadas()) {
            respuesta.add(toResponse(configuracion));
        }
        return respuesta;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ConfiguracionResponse actualizarTasa(String codigo, ActualizarTasaRequest request) {
        ConfiguracionTributaria configuracion = configuracionRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la configuración tributaria " + codigo));

        configuracion.setTasa(request.tasa());
        configuracion.setVigente(true);

        return toResponse(configuracion);
    }

    private ConfiguracionResponse toResponse(ConfiguracionTributaria configuracion) {
        return new ConfiguracionResponse(
                configuracion.getCodigo(), configuracion.getTasa(), configuracion.isVigente());
    }
}
