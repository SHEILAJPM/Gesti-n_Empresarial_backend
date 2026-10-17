package desarrolloWeb.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/* facturacion.* en application.properties (valores reales en backend/.env).
   proveedor: auto (APISUNAT si hay credenciales, si no el simulador), simulador o apisunat. */
@ConfigurationProperties(prefix = "facturacion")
public record FacturacionProperties(String proveedor, Apisunat apisunat, Emisor emisor) {

    public record Apisunat(String url, String personaId, String personaToken) {

        public boolean configurado() {
            return personaId != null && !personaId.isBlank() && personaToken != null && !personaToken.isBlank();
        }
    }

    public record Emisor(String ruc, String razonSocial, String nombreComercial, String direccion, String ubigeo) {
    }
}
