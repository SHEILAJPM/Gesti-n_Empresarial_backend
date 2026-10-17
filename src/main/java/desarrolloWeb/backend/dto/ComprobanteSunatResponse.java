package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.ComprobanteSunat;
import desarrolloWeb.backend.entity.EstadoSunat;

import java.time.LocalDateTime;

public record ComprobanteSunatResponse(
        EstadoSunat estado,
        String proveedor,
        String documentId,
        String fileName,
        String mensaje,
        String xmlUrl,
        String cdrUrl,
        String pdfUrl,
        LocalDateTime fechaEnvio) {

    public static ComprobanteSunatResponse de(ComprobanteSunat c, String pdfUrl) {
        return new ComprobanteSunatResponse(c.getEstado(), c.getProveedor(), c.getDocumentId(), c.getFileName(),
                c.getMensaje(), c.getXmlUrl(), c.getCdrUrl(), pdfUrl, c.getFechaEnvio());
    }

    public static ComprobanteSunatResponse noEnviado() {
        return new ComprobanteSunatResponse(EstadoSunat.NO_ENVIADO, null, null, null, null, null, null, null, null);
    }
}
