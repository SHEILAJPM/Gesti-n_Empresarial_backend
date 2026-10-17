package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.RespuestaFacturacion;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.util.DocumentoUbl;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/* Cliente de APISUNAT (https://docs.apisunat.com). El ambiente (DESARROLLO o PRODUCCIÓN) lo define el personaToken:
   con un token de DESARROLLO los comprobantes se generan y validan pero no se envían a SUNAT. */
public class ApisunatFacturacion implements FacturacionElectronica {

    private final RestClient http;
    private final FacturacionProperties.Apisunat credenciales;
    private final FacturacionProperties.Emisor emisor;

    public ApisunatFacturacion(FacturacionProperties propiedades) {
        this(propiedades, RestClient.builder());
    }

    /* El builder se recibe para poder probar el cliente con un servidor simulado */
    ApisunatFacturacion(FacturacionProperties propiedades, RestClient.Builder builder) {
        this.credenciales = propiedades.apisunat();
        this.emisor = propiedades.emisor();
        this.http = builder
                .baseUrl(credenciales.url())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String nombre() {
        return "APISUNAT";
    }

    @Override
    public String ambiente() {
        // El token no expone su ambiente; este proyecto se configura con un token de DESARROLLO
        return "DESARROLLO";
    }

    @Override
    public RespuestaFacturacion enviar(DocumentoElectronico documento) {
        Map<String, Object> cuerpo = credencialesBase();
        cuerpo.put("fileName", documento.fileName(emisor.ruc()));
        cuerpo.put("documentBody", DocumentoUbl.construir(documento, emisor));
        return enviarComando("/personas/v1/sendBill", cuerpo);
    }

    @Override
    public RespuestaFacturacion consultar(String documentId) {
        try {
            Map<?, ?> r = http.get().uri("/documents/{id}/getById", documentId).retrieve().body(Map.class);
            if (r == null) {
                return RespuestaFacturacion.de(EstadoSunat.EXCEPCION, documentId, "APISUNAT no devolvió respuesta");
            }
            EstadoSunat estado = estadoDe(r.get("status"));
            String mensaje = textoDe(r.get("faults"), r.get("notes"));
            return new RespuestaFacturacion(estado, documentId, mensaje.isBlank() ? descripcion(estado) : mensaje,
                    texto(r.get("xml")), texto(r.get("cdr")));
        } catch (RestClientResponseException e) {
            return RespuestaFacturacion.de(EstadoSunat.EXCEPCION, documentId, "APISUNAT: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public RespuestaFacturacion anular(String documentId, String motivo) {
        Map<String, Object> cuerpo = credencialesBase();
        cuerpo.put("documentId", documentId);
        cuerpo.put("reason", motivo.length() > 100 ? motivo.substring(0, 100) : motivo);
        return enviarComando("/personas/v1/voidBill", cuerpo);
    }

    @Override
    public int siguienteCorrelativo(String tipo, String serie, int sugeridoLocal) {
        Map<String, Object> cuerpo = credencialesBase();
        cuerpo.put("type", tipo);
        cuerpo.put("serie", serie);
        try {
            Map<?, ?> r = http.post().uri("/personas/lastDocument").contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo).retrieve().body(Map.class);
            int sugeridoApisunat = r == null || r.get("suggestedNumber") == null ? 1 : Integer.parseInt(texto(r.get("suggestedNumber")));
            // El mayor de los dos: APISUNAT puede tener documentos emitidos desde su portal
            return Math.max(sugeridoApisunat, sugeridoLocal);
        } catch (RuntimeException e) {
            return sugeridoLocal;
        }
    }

    @Override
    public String urlPdf(String documentId, String fileName) {
        return documentId == null || fileName == null ? null
                : credenciales.url() + "/documents/" + documentId + "/getPDF/A4/" + fileName + ".pdf";
    }

    private RespuestaFacturacion enviarComando(String ruta, Map<String, Object> cuerpo) {
        try {
            Map<?, ?> r = http.post().uri(ruta).contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(Map.class);
            if (r == null) {
                return RespuestaFacturacion.de(EstadoSunat.ERROR, null, "APISUNAT no devolvió respuesta");
            }
            EstadoSunat estado = estadoDe(r.get("status"));
            return RespuestaFacturacion.de(estado, texto(r.get("documentId")),
                    estado == EstadoSunat.ERROR ? "APISUNAT: " + texto(r.get("error")) : "Recibido por APISUNAT");
        } catch (RestClientResponseException e) {
            // 400: { "status": "ERROR", "error": { ... } }
            return RespuestaFacturacion.de(EstadoSunat.ERROR, null, "APISUNAT rechazó el envío: " + e.getResponseBodyAsString());
        }
    }

    private Map<String, Object> credencialesBase() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("personaId", credenciales.personaId());
        cuerpo.put("personaToken", credenciales.personaToken());
        return cuerpo;
    }

    private static EstadoSunat estadoDe(Object status) {
        try {
            return EstadoSunat.valueOf(texto(status));
        } catch (IllegalArgumentException | NullPointerException e) {
            return EstadoSunat.ERROR;
        }
    }

    private static String descripcion(EstadoSunat estado) {
        return switch (estado) {
            case ACEPTADO -> "Comprobante aceptado";
            case PENDIENTE -> "En proceso de validación";
            case RECHAZADO -> "Comprobante rechazado";
            default -> "Revisar el comprobante en APISUNAT";
        };
    }

    /* faults y notes llegan como listas de objetos o textos; se unen en un solo mensaje legible */
    private static String textoDe(Object... listas) {
        return java.util.Arrays.stream(listas)
                .filter(Collection.class::isInstance)
                .flatMap(l -> ((Collection<?>) l).stream())
                .map(MapaComoTexto::de)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining(" · "));
    }

    private static String texto(Object valor) {
        return valor == null ? null : MapaComoTexto.de(valor);
    }

    private static final class MapaComoTexto {
        static String de(Object valor) {
            if (valor instanceof Map<?, ?> mapa) {
                Object mensaje = mapa.containsKey("message") ? mapa.get("message") : mapa.get("description");
                return mensaje != null ? mensaje.toString() : mapa.toString();
            }
            return String.valueOf(valor);
        }
    }
}
