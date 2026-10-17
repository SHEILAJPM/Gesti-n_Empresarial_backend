package desarrolloWeb.backend.config;

import desarrolloWeb.backend.service.ApisunatFacturacion;
import desarrolloWeb.backend.service.FacturacionElectronica;
import desarrolloWeb.backend.service.SimuladorSunat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* Elige el proveedor de facturación electrónica:
   auto (por defecto) usa APISUNAT si hay credenciales en backend/.env y, si no, el simulador local. */
@Configuration
@EnableConfigurationProperties(FacturacionProperties.class)
public class FacturacionConfig {

    private static final Logger log = LoggerFactory.getLogger(FacturacionConfig.class);

    @Bean
    public FacturacionElectronica facturacionElectronica(FacturacionProperties propiedades) {
        String modo = propiedades.proveedor() == null ? "auto" : propiedades.proveedor().toLowerCase();
        boolean hayCredenciales = propiedades.apisunat() != null && propiedades.apisunat().configurado();

        if ("apisunat".equals(modo) && !hayCredenciales) {
            throw new IllegalStateException("facturacion.proveedor=apisunat requiere APISUNAT_PERSONA_ID y APISUNAT_PERSONA_TOKEN");
        }
        if ("apisunat".equals(modo) || ("auto".equals(modo) && hayCredenciales)) {
            log.info("Facturación electrónica: APISUNAT (emisor RUC {})", propiedades.emisor().ruc());
            return new ApisunatFacturacion(propiedades);
        }
        log.info("Facturación electrónica: simulador local de SUNAT (sin envío real)");
        return new SimuladorSunat();
    }
}
