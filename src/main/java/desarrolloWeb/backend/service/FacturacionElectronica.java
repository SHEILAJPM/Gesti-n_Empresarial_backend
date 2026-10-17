package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.RespuestaFacturacion;

/* Proveedor de facturación electrónica: el simulador local o APISUNAT. Se elige en FacturacionConfig. */
public interface FacturacionElectronica {

    /* SIMULADOR o APISUNAT */
    String nombre();

    /* DESARROLLO o PRODUCCION */
    String ambiente();

    RespuestaFacturacion enviar(DocumentoElectronico documento);

    RespuestaFacturacion consultar(String documentId);

    RespuestaFacturacion anular(String documentId, String motivo);

    /* Siguiente correlativo libre para la serie según el proveedor */
    int siguienteCorrelativo(String tipo, String serie, int sugeridoLocal);

    /* Enlace al PDF del comprobante, o null si el proveedor no lo genera */
    String urlPdf(String documentId, String fileName);
}
