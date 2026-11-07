package desarrolloWeb.backend.util;

import desarrolloWeb.backend.config.FacturacionProperties;
import desarrolloWeb.backend.dto.DocumentoElectronico;
import desarrolloWeb.backend.dto.DocumentoGuia;

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

    /* Guía de remisión remitente (DespatchAdvice 09). Antes de usarla en producción conviene compararla con el
       "Generador JSON" de APISUNAT para guías: SUNAT recibe las GRE por un canal distinto al de las facturas. */
    public static Map<String, Object> construirGuia(DocumentoGuia g, FacturacionProperties.Emisor emisor) {
        Map<String, Object> ubl = new LinkedHashMap<>();
        ubl.put("cbc:UBLVersionID", texto("2.1"));
        ubl.put("cbc:CustomizationID", texto("2.0"));
        ubl.put("cbc:ID", texto(g.numero()));
        ubl.put("cbc:IssueDate", texto(g.fechaEmision().toString()));
        ubl.put("cbc:IssueTime", texto("00:00:00"));
        ubl.put("cbc:DespatchAdviceTypeCode", conAtributos("09", Map.of("listAgencyName", "PE:SUNAT",
                "listName", "Tipo de Documento", "listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01")));
        if (g.observaciones() != null && !g.observaciones().isBlank()) {
            ubl.put("cbc:Note", List.of(texto(g.observaciones())));
        }
        if (g.relacionado() != null) {
            ubl.put("cac:AdditionalDocumentReference", List.of(Map.of(
                    "cbc:ID", texto(g.relacionado().numero()),
                    "cbc:DocumentTypeCode", texto(g.relacionado().tipo()),
                    "cbc:DocumentType", texto("01".equals(g.relacionado().tipo()) ? "Factura" : "Boleta de Venta"))));
        }
        ubl.put("cac:DespatchSupplierParty", Map.of("cac:Party", parte("6", emisor.ruc(), emisor.razonSocial(), null, null)));
        ubl.put("cac:DeliveryCustomerParty", Map.of("cac:Party", parte(g.destinatarioTipoDocumento(),
                g.destinatarioDocumento(), g.destinatarioNombre(), null, null)));

        Map<String, Object> etapa = new LinkedHashMap<>();
        // Catálogo 18: 01 transporte público, 02 privado
        etapa.put("cbc:TransportModeCode", texto(g.modalidad()));
        etapa.put("cac:TransitPeriod", Map.of("cbc:StartDate", texto(g.fechaTraslado().toString())));
        if ("01".equals(g.modalidad())) {
            etapa.put("cac:CarrierParty", Map.of(
                    "cac:PartyIdentification", Map.of("cbc:ID", conAtributos(g.transportistaRuc(), Map.of("schemeID", "6"))),
                    "cac:PartyLegalEntity", Map.of("cbc:RegistrationName", texto(g.transportistaRazonSocial()))));
        } else {
            Map<String, Object> conductor = new LinkedHashMap<>();
            conductor.put("cbc:ID", conAtributos(g.conductorDni(), Map.of("schemeID", "1")));
            conductor.put("cbc:FirstName", texto(g.conductorNombres()));
            conductor.put("cbc:FamilyName", texto(g.conductorNombres()));
            conductor.put("cbc:JobTitle", texto("Principal"));
            conductor.put("cac:IdentityDocumentReference", Map.of("cbc:ID", texto(g.conductorLicencia())));
            etapa.put("cac:DriverPerson", List.of(conductor));
        }

        Map<String, Object> envio = new LinkedHashMap<>();
        envio.put("cbc:ID", texto("SUNAT_Envio"));
        // Catálogo 20: motivo del traslado
        envio.put("cbc:HandlingCode", conAtributos(g.motivoCodigo(), Map.of("listAgencyName", "PE:SUNAT",
                "listName", "Motivo de traslado", "listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo20")));
        envio.put("cbc:HandlingInstructions", texto(g.motivoDescripcion()));
        envio.put("cbc:GrossWeightMeasure", conAtributos(g.pesoBrutoKg().stripTrailingZeros().toPlainString(),
                Map.of("unitCode", "KGM")));
        if (g.numeroBultos() != null) {
            envio.put("cbc:TotalTransportHandlingUnitQuantity", texto(String.valueOf(g.numeroBultos())));
        }
        envio.put("cac:ShipmentStage", etapa);
        envio.put("cac:Delivery", Map.of(
                "cac:DeliveryAddress", direccion(g.llegadaUbigeo(), g.llegadaDireccion()),
                "cac:Despatch", Map.of("cac:DespatchAddress", direccion(g.partidaUbigeo(), g.partidaDireccion()))));
        if ("02".equals(g.modalidad())) {
            envio.put("cac:TransportHandlingUnit", Map.of("cac:TransportEquipment",
                    Map.of("cbc:ID", texto(g.vehiculoPlaca().replace("-", "")))));
        }
        ubl.put("cac:Shipment", envio);

        List<Object> lineas = new ArrayList<>();
        for (int i = 0; i < g.lineas().size(); i++) {
            DocumentoGuia.Linea l = g.lineas().get(i);
            Map<String, Object> linea = new LinkedHashMap<>();
            linea.put("cbc:ID", texto(String.valueOf(i + 1)));
            linea.put("cbc:DeliveredQuantity", conAtributos(l.cantidad().stripTrailingZeros().toPlainString(),
                    Map.of("unitCode", l.unidad())));
            linea.put("cac:OrderLineReference", Map.of("cbc:LineID", texto(String.valueOf(i + 1))));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cbc:Description", texto(l.descripcion()));
            if (l.codigo() != null && !l.codigo().isBlank()) {
                item.put("cac:SellersItemIdentification", Map.of("cbc:ID", texto(l.codigo())));
            }
            linea.put("cac:Item", item);
            lineas.add(linea);
        }
        ubl.put("cac:DespatchLine", lineas);
        return ubl;
    }

    private static Map<String, Object> direccion(String ubigeo, String linea) {
        return Map.of(
                "cbc:ID", conAtributos(ubigeo, Map.of("schemeAgencyName", "PE:INEI", "schemeName", "Ubigeos")),
                "cac:AddressLine", Map.of("cbc:Line", texto(linea)));
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
