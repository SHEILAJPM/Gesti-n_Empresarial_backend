package desarrolloWeb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/* Datos del comprobante electrónico de una venta, tal como los devolvió el proveedor de facturación */
@Embeddable
public class ComprobanteSunat {

    @Enumerated(EnumType.STRING)
    @Column(name = "sunat_estado", nullable = false, length = 12)
    private EstadoSunat estado = EstadoSunat.NO_ENVIADO;

    // SIMULADOR o APISUNAT
    @Column(name = "sunat_proveedor", length = 12)
    private String proveedor;

    @Column(name = "sunat_document_id", length = 60)
    private String documentId;

    @Column(name = "sunat_file_name", length = 40)
    private String fileName;

    @Column(name = "sunat_mensaje", length = 500)
    private String mensaje;

    @Column(name = "sunat_xml_url", length = 300)
    private String xmlUrl;

    @Column(name = "sunat_cdr_url", length = 300)
    private String cdrUrl;

    @Column(name = "sunat_fecha_envio")
    private LocalDateTime fechaEnvio;

    public EstadoSunat getEstado() {
        return estado;
    }

    public void setEstado(EstadoSunat estado) {
        this.estado = estado;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje == null || mensaje.length() <= 500 ? mensaje : mensaje.substring(0, 497) + "...";
    }

    public String getXmlUrl() {
        return xmlUrl;
    }

    public void setXmlUrl(String xmlUrl) {
        this.xmlUrl = xmlUrl;
    }

    public String getCdrUrl() {
        return cdrUrl;
    }

    public void setCdrUrl(String cdrUrl) {
        this.cdrUrl = cdrUrl;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
