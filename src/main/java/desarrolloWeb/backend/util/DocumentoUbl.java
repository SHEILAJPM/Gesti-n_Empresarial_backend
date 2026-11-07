package desarrolloWeb.backend.util;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Arma el documentBody de APISUNAT: el XML UBL 2.1 de SUNAT expresado como JSON
   (cada etiqueta es una clave; el contenido va en "_text" y los atributos en "_attributes").
   Cubre factura/boleta gravada con IGV, con una o varias líneas, pago al contado o al crédito en cuotas,
   y notas de crédito (07) y débito (08) con referencia al comprobante que modifican.
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
        if (!d.esNota()) {
            // 0101 = venta interna (catálogo 51)
            ubl.put("cbc:InvoiceTypeCode", conAtributos(d.tipo(), Map.of("listID", "0101")));
        }
        List<Object> notas = new ArrayList<>();
        notas.add(conAtributos(MontoEnLetras.convertir(d.total()), Map.of("languageLocaleID", "1000")));
        if (d.observaciones() != null && !d.observaciones().isBlank()) {
            notas.add(texto(d.observaciones()));
        }
        ubl.put("cbc:Note", notas);
        ubl.put("cbc:DocumentCurrencyCode", texto(MONEDA));

        if (d.esNota()) {
            // Comprobante que se modifica y por qué (catálogo 09 en crédito, 10 en débito)
            DocumentoElectronico.Referencia ref = d.referencia();
            ubl.put("cac:DiscrepancyResponse", Map.of(
                    "cbc:ReferenceID", texto(ref.numero()),
                    "cbc:ResponseCode", texto(ref.motivoCodigo()),
                    "cbc:Description", texto(ref.sustento())));
            ubl.put("cac:BillingReference", Map.of("cac:InvoiceDocumentReference", Map.of(
                    "cbc:ID", texto(ref.numero()),
                    "cbc:DocumentTypeCode", texto(ref.tipoDocumento()))));
        }

        ubl.put("cac:AccountingSupplierParty", Map.of("cac:Party", parte("6", emisor.ruc(), emisor.razonSocial(),
                Map.of("cbc:AddressTypeCode", texto("0000")), emisor.nombreComercial())));
        ubl.put("cac:AccountingCustomerParty", Map.of("cac:Party", parte(d.tipoDocumentoCliente(), d.documentoCliente(),
                d.nombreCliente(), null, null)));

        if ("01".equals(d.tipo())) {
            ubl.put("cac:PaymentTerms", formaPago(d));
        }

        ubl.put("cac:TaxTotal", impuestos(d.base(), d.igv()));
        boolean debito = "08".equals(d.tipo());
        // La nota de débito usa RequestedMonetaryTotal; factura, boleta y nota de crédito, LegalMonetaryTotal
        ubl.put(debito ? "cac:RequestedMonetaryTotal" : "cac:LegalMonetaryTotal", Map.of(
                "cbc:LineExtensionAmount", monto(d.base()),
                "cbc:TaxInclusiveAmount", monto(d.total()),
                "cbc:PayableAmount", monto(d.total())));
        String etiquetaLinea = "07".equals(d.tipo()) ? "cac:CreditNoteLine" : debito ? "cac:DebitNoteLine" : "cac:InvoiceLine";
        String etiquetaCantidad = "07".equals(d.tipo()) ? "cbc:CreditedQuantity" : debito ? "cbc:DebitedQuantity" : "cbc:InvoicedQuantity";
        List<Object> lineas = new ArrayList<>();
        for (int i = 0; i < d.lineas().size(); i++) {
            lineas.add(linea(i + 1, d.lineas().get(i), d.tasaIgv(), etiquetaCantidad));
        }
        ubl.put(etiquetaLinea, lineas);
        return ubl;
    }

    /* Desde 2021 la factura indica la forma de pago. Al crédito: el monto pendiente y cada cuota (Cuota001, ...) */
    static List<Object> formaPago(DocumentoElectronico d) {
        List<Object> terminos = new ArrayList<>();
        if (!"Credito".equals(d.formaPago()) || d.cuotas().isEmpty()) {
            terminos.add(Map.of("cbc:ID", texto("FormaPago"), "cbc:PaymentMeansID", texto("Contado")));
            return terminos;
        }
        Map<String, Object> credito = new LinkedHashMap<>();
        credito.put("cbc:ID", texto("FormaPago"));
        credito.put("cbc:PaymentMeansID", texto("Credito"));
        credito.put("cbc:Amount", monto(d.total()));
        terminos.add(credito);
        for (DocumentoElectronico.CuotaPago c : d.cuotas()) {
            Map<String, Object> cuota = new LinkedHashMap<>();
            cuota.put("cbc:ID", texto("FormaPago"));
            cuota.put("cbc:PaymentMeansID", texto(String.format("Cuota%03d", c.numero())));
            cuota.put("cbc:Amount", monto(c.monto()));
            cuota.put("cbc:PaymentDueDate", texto(c.fechaVencimiento().toString()));
            terminos.add(cuota);
        }
        return terminos;
    }

    private static Map<String, Object> linea(int numero, DocumentoElectronico.Linea l, BigDecimal tasa,
                                             String etiquetaCantidad) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("cbc:ID", texto(String.valueOf(numero)));
        // unitCode: catálogo 03 (NIU unidad, ZZ servicio, KGM kilogramo, ...)
        linea.put(etiquetaCantidad, conAtributos(l.cantidad().stripTrailingZeros().toPlainString(),
                Map.of("unitCode", l.unidad())));
        linea.put("cbc:LineExtensionAmount", monto(l.valorVenta()));
        // 01 = precio unitario con IGV (catálogo 16)
        BigDecimal precioConIgv = l.valorVenta().add(l.igv()).divide(l.cantidad(), 2, RoundingMode.HALF_UP);
        linea.put("cac:PricingReference", Map.of("cac:AlternativeConditionPrice", Map.of(
                "cbc:PriceAmount", monto(precioConIgv),
                "cbc:PriceTypeCode", texto("01"))));
        linea.put("cac:TaxTotal", impuestos(l.valorVenta(), l.igv(), tasa));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("cbc:Description", texto(l.descripcion()));
        if (l.codigo() != null && !l.codigo().isBlank()) {
            item.put("cac:SellersItemIdentification", Map.of("cbc:ID", texto(l.codigo())));
        }
        linea.put("cac:Item", item);
        // Valor unitario sin IGV
        linea.put("cac:Price", Map.of("cbc:PriceAmount", monto(l.valorUnitario())));
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
