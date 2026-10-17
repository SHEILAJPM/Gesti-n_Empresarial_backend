package desarrolloWeb.backend.util;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Arma el documentBody de APISUNAT: el XML UBL 2.1 de SUNAT expresado como JSON
   (cada etiqueta es una clave; el contenido va en "_text" y los atributos en "_attributes").
   Cubre factura/boleta gravada con IGV, una línea y pago al contado.
   Para otros casos, comparar con el "Generador JSON" del portal APISUNAT (botón { } al emitir). */
public final class DocumentoUbl {

    private static final String MONEDA = "PEN";

    private DocumentoUbl() {
    }

    public static Map<String, Object> construir(DocumentoElectronico d, FacturacionProperties.Emisor emisor) {
        Map<String, Object> ubl = new LinkedHashMap<>();
        ubl.put("cbc:UBLVersionID", texto("2.1"));
        ubl.put("cbc:CustomizationID", texto("2.0"));
        ubl.put("cbc:ID", texto(d.numero()));
        ubl.put("cbc:IssueDate", texto(d.fechaEmision().toString()));
        ubl.put("cbc:IssueTime", texto("00:00:00"));
        // 0101 = venta interna (catálogo 51)
        ubl.put("cbc:InvoiceTypeCode", conAtributos(d.tipo(), Map.of("listID", "0101")));
        ubl.put("cbc:Note", List.of(conAtributos(MontoEnLetras.convertir(d.total()), Map.of("languageLocaleID", "1000"))));
        ubl.put("cbc:DocumentCurrencyCode", texto(MONEDA));

        ubl.put("cac:AccountingSupplierParty", Map.of("cac:Party", parte("6", emisor.ruc(), emisor.razonSocial(),
                Map.of("cbc:AddressTypeCode", texto("0000")), emisor.nombreComercial())));
        ubl.put("cac:AccountingCustomerParty", Map.of("cac:Party", parte(d.tipoDocumentoCliente(), d.documentoCliente(),
                d.nombreCliente(), null, null)));

        if ("01".equals(d.tipo())) {
            // Desde 2021 la factura debe indicar la forma de pago
            ubl.put("cac:PaymentTerms", List.of(Map.of("cbc:ID", texto("FormaPago"), "cbc:PaymentMeansID", texto("Contado"))));
        }

        ubl.put("cac:TaxTotal", impuestos(d.base(), d.igv()));
        ubl.put("cac:LegalMonetaryTotal", Map.of(
                "cbc:LineExtensionAmount", monto(d.base()),
                "cbc:TaxInclusiveAmount", monto(d.total()),
                "cbc:PayableAmount", monto(d.total())));
        ubl.put("cac:InvoiceLine", List.of(linea(d)));
        return ubl;
    }

    private static Map<String, Object> linea(DocumentoElectronico d) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("cbc:ID", texto("1"));
        // ZZ = servicio / unidad genérica (catálogo 03)
        linea.put("cbc:InvoicedQuantity", conAtributos("1", Map.of("unitCode", "ZZ")));
        linea.put("cbc:LineExtensionAmount", monto(d.base()));
        // 01 = precio unitario con IGV (catálogo 16)
        linea.put("cac:PricingReference", Map.of("cac:AlternativeConditionPrice", Map.of(
                "cbc:PriceAmount", monto(d.total()),
                "cbc:PriceTypeCode", texto("01"))));
        linea.put("cac:TaxTotal", impuestos(d.base(), d.igv(), d.tasaIgv()));
        linea.put("cac:Item", Map.of("cbc:Description", texto(d.descripcion())));
        linea.put("cac:Price", Map.of("cbc:PriceAmount", monto(d.base())));
        return linea;
    }

    private static Map<String, Object> impuestos(BigDecimal base, BigDecimal igv) {
        return impuestos(base, igv, null);
    }

    /* IGV (código 1000, VAT); con tasa se agrega el porcentaje y la afectación 10 = gravado oneroso (catálogo 07) */
    private static Map<String, Object> impuestos(BigDecimal base, BigDecimal igv, BigDecimal tasa) {
        Map<String, Object> categoria = new LinkedHashMap<>();
        if (tasa != null) {
            categoria.put("cbc:Percent", texto(tasa.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()));
            categoria.put("cbc:TaxExemptionReasonCode", texto("10"));
        }
        categoria.put("cac:TaxScheme", Map.of(
                "cbc:ID", texto("1000"),
                "cbc:Name", texto("IGV"),
                "cbc:TaxTypeCode", texto("VAT")));
        return Map.of(
                "cbc:TaxAmount", monto(igv),
                "cac:TaxSubtotal", List.of(Map.of(
                        "cbc:TaxableAmount", monto(base),
                        "cbc:TaxAmount", monto(igv),
                        "cac:TaxCategory", categoria)));
    }

    private static Map<String, Object> parte(String tipoDocumento, String numero, String nombre,
                                             Map<String, Object> direccion, String nombreComercial) {
        Map<String, Object> entidad = new LinkedHashMap<>();
        entidad.put("cbc:RegistrationName", texto(nombre));
        if (direccion != null) {
            entidad.put("cac:RegistrationAddress", direccion);
        }
        Map<String, Object> parte = new LinkedHashMap<>();
        parte.put("cac:PartyIdentification", Map.of("cbc:ID", conAtributos(numero, Map.of("schemeID", tipoDocumento))));
        if (nombreComercial != null && !nombreComercial.isBlank()) {
            parte.put("cac:PartyName", Map.of("cbc:Name", texto(nombreComercial)));
        }
        parte.put("cac:PartyLegalEntity", entidad);
        return parte;
    }

    private static Map<String, Object> monto(BigDecimal valor) {
        return conAtributos(valor.setScale(2, RoundingMode.HALF_UP).toPlainString(), Map.of("currencyID", MONEDA));
    }

    private static Map<String, Object> texto(String valor) {
        return Map.of("_text", valor);
    }

    private static Map<String, Object> conAtributos(String valor, Map<String, String> atributos) {
        Map<String, Object> nodo = new LinkedHashMap<>();
        nodo.put("_attributes", atributos);
        nodo.put("_text", valor);
        return nodo;
    }
}
