package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.DocumentoConsultado;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ServicioExternoException;
import desarrolloWeb.backend.util.ValidadorDocumento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/* Nombre de una persona (RENIEC, por DNI) o razón social de una empresa (SUNAT, por RUC) con la API de Decolecta.
   El token vive solo en el backend (DECOLECTA_TOKEN en backend/.env); el navegador nunca lo ve. */
@Service
public class ConsultaDocumentoService {

    private static final int MAXIMO_CACHE = 1000;

    private final RestClient http;
    private final String token;
    private final ValidadorDocumento validador = new ValidadorDocumento();
    // Cada consulta gasta cuota del plan: lo ya consultado se responde de memoria
    private final Map<String, DocumentoConsultado> cache = new ConcurrentHashMap<>();

    @Autowired
    public ConsultaDocumentoService(@Value("${consulta.decolecta.url}") String url,
                                    @Value("${consulta.decolecta.token:}") String token) {
        this(url, token, RestClient.builder().requestFactory(fabricaConTimeouts()));
    }

    /* El builder se recibe para poder probar el cliente con un servidor simulado */
    ConsultaDocumentoService(String url, String token, RestClient.Builder builder) {
        this.token = token;
        this.http = builder
                .baseUrl(url)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean configurado() {
        return token != null && !token.isBlank();
    }

    public DocumentoConsultado consultar(String numero) {
        String documento = numero == null ? "" : numero.trim();
        TipoDocumento tipo = validador.tipoDocumentoDe(documento);
        if (!configurado()) {
            throw new ServicioExternoException(
                    "La consulta de DNI/RUC no está configurada: agrega DECOLECTA_TOKEN en backend/.env");
        }
        DocumentoConsultado guardado = cache.get(documento);
        if (guardado != null) {
            return guardado;
        }
        DocumentoConsultado resultado = tipo == TipoDocumento.DNI ? consultarDni(documento) : consultarRuc(documento);
        if (cache.size() >= MAXIMO_CACHE) {
            cache.clear();
        }
        cache.put(documento, resultado);
        return resultado;
    }

    private DocumentoConsultado consultarDni(String dni) {
        Map<?, ?> r = pedir("/v1/reniec/dni", dni);
        String nombre = Arrays.stream(new Object[]{r.get("first_name"), r.get("first_last_name"), r.get("second_last_name")})
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(t -> !t.isBlank())
                .collect(Collectors.joining(" "));
        if (nombre.isBlank()) {
            nombre = texto(r.get("full_name"));
        }
        if (nombre == null || nombre.isBlank()) {
            throw new RecursoNoEncontradoException("RENIEC no tiene datos para el DNI " + dni);
        }
        return new DocumentoConsultado(dni, TipoDocumento.DNI, capitalizar(nombre), null, null, null);
    }

    private DocumentoConsultado consultarRuc(String ruc) {
        Map<?, ?> r = pedir("/v1/sunat/ruc", ruc);
        String razonSocial = texto(r.get("razon_social"));
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new RecursoNoEncontradoException("SUNAT no tiene datos para el RUC " + ruc);
        }
        return new DocumentoConsultado(ruc, TipoDocumento.RUC, razonSocial.trim(), direccionDe(r),
                texto(r.get("estado")), texto(r.get("condicion")));
    }

    private Map<?, ?> pedir(String ruta, String numero) {
        try {
            Map<?, ?> r = http.get()
                    .uri(u -> u.path(ruta).queryParam("numero", numero).build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (r == null) {
                throw new RecursoNoEncontradoException("No se encontraron datos para " + numero);
            }
            return r;
        } catch (RestClientResponseException e) {
            int codigo = e.getStatusCode().value();
            if (codigo == 400 || codigo == 404 || codigo == 422) {
                throw new RecursoNoEncontradoException("No se encontraron datos para el documento " + numero);
            }
            if (codigo == 401 || codigo == 403) {
                throw new ServicioExternoException("El token de Decolecta no es válido o venció (DECOLECTA_TOKEN)");
            }
            if (codigo == 429) {
                throw new ServicioExternoException("Se alcanzó el límite de consultas de DNI/RUC del plan; intenta más tarde");
            }
            throw new ServicioExternoException("El servicio de consulta de DNI/RUC respondió con error " + codigo);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo conectar con el servicio de consulta de DNI/RUC");
        }
    }

    /* "AV. JOSE GALVEZ 566 " + distrito, provincia y departamento, sin los "-" que SUNAT pone en campos vacíos */
    private static String direccionDe(Map<?, ?> r) {
        String calle = texto(r.get("direccion"));
        if (calle == null || calle.isBlank() || calle.trim().equals("-")) {
            return null;
        }
        String ubicacion = Arrays.stream(new Object[]{r.get("distrito"), r.get("provincia"), r.get("departamento")})
                .map(ConsultaDocumentoService::texto)
                .filter(t -> t != null && !t.isBlank() && !t.equals("-"))
                .collect(Collectors.joining(", "));
        String completa = calle.trim().replaceAll("\\s+", " ") + (ubicacion.isEmpty() ? "" : ", " + ubicacion);
        return completa.length() > 200 ? completa.substring(0, 200) : completa;
    }

    /* RENIEC devuelve todo en mayúsculas: "ROXANA KARINA" -> "Roxana Karina" */
    static String capitalizar(String nombre) {
        return Arrays.stream(nombre.trim().toLowerCase(Locale.forLanguageTag("es-PE")).split("\\s+"))
                .map(p -> p.isEmpty() ? p : Character.toUpperCase(p.charAt(0)) + p.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }

    private static JdkClientHttpRequestFactory fabricaConTimeouts() {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(6));
        return fabrica;
    }
}
