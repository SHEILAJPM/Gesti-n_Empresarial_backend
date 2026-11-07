package desarrolloWeb.backend.service;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.ComprobanteSunatResponse;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.DocumentoGuia;
import desarrolloWeb.backend.entity.Guia;
import desarrolloWeb.backend.util.MotivoTraslado;
import desarrolloWeb.backend.dto.RespuestaFacturacion;
import desarrolloWeb.backend.entity.ComprobanteSunat;
import desarrolloWeb.backend.entity.EstadoSunat;
import desarrolloWeb.backend.entity.FormaPago;
import desarrolloWeb.backend.entity.Nota;
import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
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
        enviar(venta.getSunat(), documentoDe(venta), venta.getNumeroComprobante());
    }

    public void emitir(Nota nota) {
        enviar(nota.getSunat(), documentoDe(nota), nota.getNumero());
    }

    public void emitir(Guia guia) {
        DocumentoGuia documento = documentoDe(guia);
        enviar(guia.getSunat(), documento.fileName(propiedades.emisor().ruc()), guia.getNumero(),
                () -> proveedor.enviarGuia(documento));
    }

    public void consultar(Venta venta) {
        consultar(venta.getSunat());
    }

    public void consultar(Nota nota) {
        consultar(nota.getSunat());
    }

    public void consultar(Guia guia) {
        consultar(guia.getSunat());
    }

    public ComprobanteSunatResponse respuesta(Nota nota) {
        return respuesta(nota.getSunat());
    }

    public ComprobanteSunatResponse respuesta(Guia guia) {
        return respuesta(guia.getSunat());
    }

    private ComprobanteSunatResponse respuesta(ComprobanteSunat c) {
        String pdf = c.getEstado() == EstadoSunat.ACEPTADO ? proveedor.urlPdf(c.getDocumentId(), c.getFileName()) : null;
        return ComprobanteSunatResponse.de(c, pdf);
    }

    /* Guía remitente: motivo (catálogo 20), traslado y, si viene de una venta, su comprobante */
    DocumentoGuia documentoDe(Guia g) {
        String[] partes = g.getNumero().split("-");
        DocumentoGuia.DocumentoRelacionado relacionado = null;
        if (g.getVenta() != null) {
            String[] v = g.getVenta().getNumeroComprobante().split("-");
            relacionado = new DocumentoGuia.DocumentoRelacionado(
                    g.getVenta().getTipoComprobante() == TipoComprobante.FACTURA ? "01" : "03",
                    v[0] + "-" + String.format("%08d", Integer.parseInt(v[1])));
        }
        List<DocumentoGuia.Linea> lineas = g.getItems().stream()
                .map(i -> new DocumentoGuia.Linea(i.getCodigo(), i.getDescripcion(), i.getUnidad(), i.getCantidad()))
                .toList();
        return new DocumentoGuia(partes[0], Integer.parseInt(partes[1]), g.getFechaEmision(), g.getFechaTraslado(),
                g.getMotivoCodigo(), MotivoTraslado.CATALOGO.get(g.getMotivoCodigo()), g.getDestinatarioTipoDocumento(),
                g.getDestinatarioDocumento(), g.getDestinatarioNombre(), g.getPesoBrutoKg(), g.getNumeroBultos(),
                g.getModalidad().getCodigoSunat(), g.getTransportistaRuc(), g.getTransportistaRazonSocial(),
                g.getVehiculoPlaca(), g.getConductorDni(), g.getConductorNombres(), g.getConductorLicencia(),
                g.getPartidaUbigeo(), g.getPartidaDireccion(), g.getLlegadaUbigeo(), g.getLlegadaDireccion(),
                g.getObservaciones(), relacionado, lineas);
    }

    /* Envía cualquier comprobante (factura, boleta o nota) y deja en c el resultado */
    private void enviar(ComprobanteSunat c, DocumentoElectronico documento, String numero) {
        enviar(c, documento.fileName(propiedades.emisor().ruc()), numero, () -> proveedor.enviar(documento));
    }

    private void enviar(ComprobanteSunat c, String fileName, String numero, Supplier<RespuestaFacturacion> envio) {
        if (c.getEstado() == EstadoSunat.ACEPTADO || c.getEstado() == EstadoSunat.PENDIENTE) {
            throw new ReglaNegocioException("El comprobante " + numero + " ya fue enviado");
        }
        c.setProveedor(proveedor.nombre());
        c.setFileName(fileName);
        c.setFechaEnvio(LocalDateTime.now());
        c.setXmlUrl(null);
        c.setCdrUrl(null);
        try {
            RespuestaFacturacion r = envio.get();
            c.setEstado(r.estado());
            c.setDocumentId(r.documentId());
            c.setMensaje(r.mensaje());
            if (r.estado() == EstadoSunat.PENDIENTE && r.documentId() != null) {
                // Suele resolverse en segundos; si sigue pendiente se consulta después
                consultar(c);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo emitir {}: {}", numero, e.getMessage());
            c.setEstado(EstadoSunat.ERROR);
            c.setMensaje("No se pudo conectar con " + proveedor.nombre() + ": " + e.getMessage());
        }
    }

    /* Nota de crédito/débito: mismas reglas de documento, con la referencia a la factura o boleta original */
    DocumentoElectronico documentoDe(Nota nota) {
        Venta venta = nota.getVenta();
        String[] partes = nota.getNumero().split("-");
        boolean clienteRuc = venta.getCliente().getTipoDocumento() == TipoDocumento.RUC;
        List<DocumentoElectronico.Linea> lineas = nota.getItems().stream()
                .map(i -> new DocumentoElectronico.Linea(i.getCodigo(), i.getDescripcion(), i.getUnidad(),
                        i.getCantidad(), i.getValorUnitario(), i.getValorVenta(), i.getIgv()))
                .toList();
        String tipoAfectado = venta.getTipoComprobante() == TipoComprobante.FACTURA ? "01" : "03";
        String[] afectado = venta.getNumeroComprobante().split("-");
        String numeroAfectado = afectado[0] + "-" + String.format("%08d", Integer.parseInt(afectado[1]));
        return new DocumentoElectronico(nota.getTipo().getCodigoSunat(), partes[0], Integer.parseInt(partes[1]),
                nota.getFecha(), clienteRuc ? "6" : "1", venta.getCliente().getDocumento(), venta.getCliente().getNombre(),
                nota.getSustento(), nota.getMontoSinIgv(), nota.getIgv(), nota.getMontoTotal(), nota.getTasaAplicada(),
                lineas, "Contado", List.of(), null,
                new DocumentoElectronico.Referencia(tipoAfectado, numeroAfectado, nota.getMotivoCodigo(), nota.getSustento()));
    }

    private void consultar(ComprobanteSunat c) {
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
        if (!venta.getItems().isEmpty()) {
            List<DocumentoElectronico.Linea> lineas = venta.getItems().stream()
                    .map(i -> new DocumentoElectronico.Linea(i.getCodigo(), i.getDescripcion(), i.getUnidad(),
                            i.getCantidad(), i.getValorUnitario(), i.getValorVenta(), i.getIgv()))
                    .toList();
            List<DocumentoElectronico.CuotaPago> cuotas = venta.getCuotas().stream()
                    .map(c -> new DocumentoElectronico.CuotaPago(c.getNumero(), c.getFechaVencimiento(), c.getMonto()))
                    .toList();
            return new DocumentoElectronico(factura ? "01" : "03", partes[0], Integer.parseInt(partes[1]),
                    venta.getFecha(), clienteRuc ? "6" : "1", venta.getCliente().getDocumento(),
                    venta.getCliente().getNombre(), DESCRIPCION_ITEM, venta.getMontoSinIgv(), venta.getIgv(),
                    venta.getMontoTotal(), venta.getTasaAplicada(), lineas,
                    venta.getFormaPago() == FormaPago.CREDITO ? "Credito" : "Contado", cuotas, venta.getObservaciones(), null);
        }
        // Ventas registradas sin detalle: una línea genérica por el total
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
