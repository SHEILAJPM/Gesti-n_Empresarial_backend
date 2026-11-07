package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* Comprobante a emitir, independiente del proveedor de facturación.
   tipo: 01 factura, 03 boleta. tipoDocumentoCliente: 6 RUC, 1 DNI (catálogo 06 de SUNAT).
   formaPago: "Contado" o "Credito" (con sus cuotas). */
public record DocumentoElectronico(
        String tipo,
        String serie,
        int correlativo,
        LocalDate fechaEmision,
        String tipoDocumentoCliente,
        String documentoCliente,
        String nombreCliente,
        String descripcion,
        BigDecimal base,
        BigDecimal igv,
        BigDecimal total,
        BigDecimal tasaIgv,
        List<Linea> lineas,
        String formaPago,
        List<CuotaPago> cuotas,
        String observaciones,
        Referencia referencia) {

    /* Solo en notas (07/08): el comprobante que modifican y el motivo (catálogo 09 o 10) */
    public record Referencia(String tipoDocumento, String numero, String motivoCodigo, String sustento) {
    }

    public boolean esNota() {
        return "07".equals(tipo) || "08".equals(tipo);
    }

    /* unidad: catálogo 03 · valorUnitario sin IGV · valorVenta = cantidad × valorUnitario */
    public record Linea(String codigo, String descripcion, String unidad, BigDecimal cantidad,
                        BigDecimal valorUnitario, BigDecimal valorVenta, BigDecimal igv) {
    }

    public record CuotaPago(int numero, LocalDate fechaVencimiento, BigDecimal monto) {
    }

    /* Comprobante sin detalle (ventas anteriores): una línea genérica por el total, al contado */
    public DocumentoElectronico(String tipo, String serie, int correlativo, LocalDate fechaEmision,
                                String tipoDocumentoCliente, String documentoCliente, String nombreCliente,
                                String descripcion, BigDecimal base, BigDecimal igv, BigDecimal total,
                                BigDecimal tasaIgv) {
        this(tipo, serie, correlativo, fechaEmision, tipoDocumentoCliente, documentoCliente, nombreCliente, descripcion,
                base, igv, total, tasaIgv,
                List.of(new Linea(null, descripcion, "ZZ", BigDecimal.ONE, base, base, igv)),
                "Contado", List.of(), null, null);
    }

    /* F001-00000012 */
    public String numero() {
        return serie + "-" + String.format("%08d", correlativo);
    }

    /* RUC-TT-SERIE-CORRELATIVO, el formato que exige SUNAT para el XML */
    public String fileName(String rucEmisor) {
        return rucEmisor + "-" + tipo + "-" + numero();
    }
}
