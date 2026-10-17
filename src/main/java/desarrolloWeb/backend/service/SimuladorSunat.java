package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.RespuestaFacturacion;
import desarrolloWeb.backend.entity.EstadoSunat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/* Simula a SUNAT sin conexión: aplica las validaciones básicas de un CPE y responde como un proveedor real
   (envío PENDIENTE, luego ACEPTADO o RECHAZADO al consultar). No genera XML, CDR ni PDF.
   El veredicto viaja dentro del documentId, así no necesita guardar estado propio. */
public class SimuladorSunat implements FacturacionElectronica {

    private static final String PREFIJO_OK = "SIM-OK-";
    private static final String PREFIJO_RECHAZO = "SIM-R";
    private static final int PLAZO_DIAS = 3;

    static final List<String> RECHAZOS = List.of(
            "La factura solo se emite a clientes con RUC (11 dígitos).",
            "La boleta requiere un DNI de 8 dígitos o RUC del cliente.",
            "La serie no corresponde al tipo de comprobante (F para facturas, B para boletas).",
            "El importe total no coincide con la base imponible más el IGV.",
            "La fecha de emisión no puede ser posterior a hoy.",
            "La fecha de emisión supera el plazo de " + PLAZO_DIAS + " días para enviar el comprobante.");

    @Override
    public String nombre() {
        return "SIMULADOR";
    }

    @Override
    public String ambiente() {
        return "DESARROLLO";
    }

    @Override
    public RespuestaFacturacion enviar(DocumentoElectronico d) {
        List<Integer> errores = validar(d);
        String id = errores.isEmpty()
                ? PREFIJO_OK + UUID.randomUUID()
                : PREFIJO_RECHAZO + errores.get(0) + "-" + UUID.randomUUID();
        return RespuestaFacturacion.de(EstadoSunat.PENDIENTE, id, "Recibido por el simulador; consulta el estado para ver la respuesta.");
    }

    @Override
    public RespuestaFacturacion consultar(String documentId) {
        if (documentId.startsWith(PREFIJO_OK)) {
            return RespuestaFacturacion.de(EstadoSunat.ACEPTADO, documentId,
                    "El comprobante ha sido aceptado (simulación: no se envió a SUNAT).");
        }
        if (documentId.startsWith(PREFIJO_RECHAZO)) {
            int indice = Integer.parseInt(documentId.substring(PREFIJO_RECHAZO.length(), documentId.indexOf('-', PREFIJO_RECHAZO.length())));
            return RespuestaFacturacion.de(EstadoSunat.RECHAZADO, documentId, RECHAZOS.get(indice));
        }
        return RespuestaFacturacion.de(EstadoSunat.EXCEPCION, documentId, "El simulador no reconoce este documento.");
    }

    @Override
    public RespuestaFacturacion anular(String documentId, String motivo) {
        return RespuestaFacturacion.de(EstadoSunat.ACEPTADO, documentId, "Comunicación de baja aceptada (simulación). Motivo: " + motivo);
    }

    @Override
    public int siguienteCorrelativo(String tipo, String serie, int sugeridoLocal) {
        return sugeridoLocal;
    }

    @Override
    public String urlPdf(String documentId, String fileName) {
        return null;
    }

    static List<Integer> validar(DocumentoElectronico d) {
        List<Integer> errores = new ArrayList<>();
        boolean esFactura = "01".equals(d.tipo());
        if (esFactura && !("6".equals(d.tipoDocumentoCliente()) && d.documentoCliente().matches("\\d{11}"))) {
            errores.add(0);
        }
        if (!esFactura && !d.documentoCliente().matches("\\d{8}|\\d{11}")) {
            errores.add(1);
        }
        if (!d.serie().startsWith(esFactura ? "F" : "B")) {
            errores.add(2);
        }
        if (d.base().add(d.igv()).subtract(d.total()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            errores.add(3);
        }
        LocalDate hoy = LocalDate.now();
        if (d.fechaEmision().isAfter(hoy)) {
            errores.add(4);
        } else if (d.fechaEmision().isBefore(hoy.minusDays(PLAZO_DIAS))) {
            errores.add(5);
        }
        return errores;
    }
}
