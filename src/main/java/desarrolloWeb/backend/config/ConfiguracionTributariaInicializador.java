package desarrolloWeb.backend.config;

import desarrolloWeb.backend.entity.ConfiguracionTributaria;
import desarrolloWeb.backend.repository.ConfiguracionTributariaRepository;
import desarrolloWeb.backend.service.ConfiguracionTributariaService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ConfiguracionTributariaInicializador implements ApplicationRunner {

    private final ConfiguracionTributariaRepository configuracionRepository;

    public ConfiguracionTributariaInicializador(ConfiguracionTributariaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (configuracionRepository.findByCodigo(ConfiguracionTributariaService.CODIGO_IGV).isEmpty()) {
            configuracionRepository.save(new ConfiguracionTributaria(
                    ConfiguracionTributariaService.CODIGO_IGV, new BigDecimal("0.1800"), true));
        }
    }
}
