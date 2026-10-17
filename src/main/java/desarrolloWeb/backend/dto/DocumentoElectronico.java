package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/* Comprobante a emitir, independiente del proveedor de facturación.
   tipo: 01 factura, 03 boleta. tipoDocumentoCliente: 6 RUC, 1 DNI (catálogo 06 de SUNAT). */
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
        BigDecimal tasaIgv) {

    /* F001-00000012 */
    public String numero() {
        return serie + "-" + String.format("%08d", correlativo);
    }

    /* RUC-TT-SERIE-CORRELATIVO, el formato que exige SUNAT para el XML */
    public String fileName(String rucEmisor) {
        return rucEmisor + "-" + tipo + "-" + numero();
    }
}
