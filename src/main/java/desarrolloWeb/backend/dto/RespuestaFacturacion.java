package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.EstadoSunat;

/* Respuesta normalizada de cualquier proveedor de facturación (simulador o APISUNAT) */
public record RespuestaFacturacion(
        EstadoSunat estado,
        String documentId,
        String mensaje,
        String xmlUrl,
        String cdrUrl) {

    public static RespuestaFacturacion de(EstadoSunat estado, String documentId, String mensaje) {
        return new RespuestaFacturacion(estado, documentId, mensaje, null, null);
    }
}
