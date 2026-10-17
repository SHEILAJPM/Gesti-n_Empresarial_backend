package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.ComprobanteSunatResponse;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.RespuestaFacturacion;
import desarrolloWeb.backend.entity.ComprobanteSunat;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/* Emite, consulta y anula el comprobante electrónico de una venta con el proveedor configurado.
   Un fallo del proveedor nunca impide registrar la venta: queda en estado ERROR y se puede reenviar. */
@Service
public class FacturacionService {

    private static final Logger log = LoggerFactory.getLogger(FacturacionService.class);
    private static final String DESCRIPCION_ITEM = "Venta de bienes y/o servicios";

    private final FacturacionElectronica proveedor;
    private final FacturacionProperties propiedades;

    public FacturacionService(FacturacionElectronica proveedor, FacturacionProperties propiedades) {
        this.proveedor = proveedor;
        this.propiedades = propiedades;
    }

    public void emitir(Venta venta) {
        ComprobanteSunat c = venta.getSunat();
        if (c.getEstado() == EstadoSunat.ACEPTADO || c.getEstado() == EstadoSunat.PENDIENTE) {
            throw new ReglaNegocioException("El comprobante " + venta.getNumeroComprobante() + " ya fue enviado");
        }
        DocumentoElectronico documento = documentoDe(venta);
        c.setProveedor(proveedor.nombre());
        c.setFileName(documento.fileName(propiedades.emisor().ruc()));
        c.setFechaEnvio(LocalDateTime.now());
        c.setXmlUrl(null);
        c.setCdrUrl(null);
        try {
            RespuestaFacturacion r = proveedor.enviar(documento);
            c.setEstado(r.estado());
            c.setDocumentId(r.documentId());
            c.setMensaje(r.mensaje());
            if (r.estado() == EstadoSunat.PENDIENTE && r.documentId() != null) {
                // Suele resolverse en segundos; si sigue pendiente se consulta después
                consultar(venta);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo emitir {}: {}", venta.getNumeroComprobante(), e.getMessage());
            c.setEstado(EstadoSunat.ERROR);
            c.setMensaje("No se pudo conectar con " + proveedor.nombre() + ": " + e.getMessage());
        }
    }

    public void consultar(Venta venta) {
        ComprobanteSunat c = venta.getSunat();
        if (c.getDocumentId() == null) {
            return;
        }
        try {
            RespuestaFacturacion r = proveedor.consultar(c.getDocumentId());
            c.setEstado(r.estado());
            c.setMensaje(r.mensaje());
            if (r.xmlUrl() != null) c.setXmlUrl(r.xmlUrl());
            if (r.cdrUrl() != null) c.setCdrUrl(r.cdrUrl());
        } catch (RuntimeException e) {
            c.setMensaje("No se pudo consultar a " + proveedor.nombre() + ": " + e.getMessage());
        }
    }

    /* Comunicación de baja: solo aplica a comprobantes aceptados */
    public void anular(Venta venta, String motivo) {
        ComprobanteSunat c = venta.getSunat();
        if (c.getEstado() != EstadoSunat.ACEPTADO || c.getDocumentId() == null) {
            return;
        }
        try {
            RespuestaFacturacion r = proveedor.anular(c.getDocumentId(), motivo);
            c.setMensaje("Baja enviada (" + r.estado() + "): " + r.mensaje());
        } catch (RuntimeException e) {
            c.setMensaje("La venta se anuló pero la baja no llegó a " + proveedor.nombre() + ": " + e.getMessage());
        }
    }

    public int siguienteCorrelativo(String tipo, String serie, int sugeridoLocal) {
        return proveedor.siguienteCorrelativo(tipo, serie, sugeridoLocal);
    }

    public ComprobanteSunatResponse respuesta(Venta venta) {
        ComprobanteSunat c = venta.getSunat();
        String pdf = c.getEstado() == EstadoSunat.ACEPTADO ? proveedor.urlPdf(c.getDocumentId(), c.getFileName()) : null;
        return ComprobanteSunatResponse.de(c, pdf);
    }

    public Map<String, Object> estado() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("proveedor", proveedor.nombre());
        estado.put("ambiente", proveedor.ambiente());
        estado.put("emisorRuc", propiedades.emisor().ruc());
        estado.put("emisorRazonSocial", propiedades.emisor().razonSocial());
        return estado;
    }

    DocumentoElectronico documentoDe(Venta venta) {
        String[] partes = venta.getNumeroComprobante().split("-");
        boolean factura = venta.getTipoComprobante() == TipoComprobante.FACTURA;
        boolean clienteRuc = venta.getCliente().getTipoDocumento() == TipoDocumento.RUC;
        return new DocumentoElectronico(
                factura ? "01" : "03",
                partes[0],
                Integer.parseInt(partes[1]),
                venta.getFecha(),
                clienteRuc ? "6" : "1",
                venta.getCliente().getDocumento(),
                venta.getCliente().getNombre(),
                DESCRIPCION_ITEM,
                venta.getMontoSinIgv(),
                venta.getIgv(),
                venta.getMontoTotal(),
                venta.getTasaAplicada());
    }
}
