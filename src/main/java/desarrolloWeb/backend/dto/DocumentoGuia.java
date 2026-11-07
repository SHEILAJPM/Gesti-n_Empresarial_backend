package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* Guía de remisión remitente (tipo 09) a emitir, independiente del proveedor de facturación.
   modalidad: 01 transporte público (con transportista) · 02 privado (con vehículo y conductor). */
public record DocumentoGuia(
        String serie,
        int correlativo,
        LocalDate fechaEmision,
        LocalDate fechaTraslado,
        String motivoCodigo,
        String motivoDescripcion,
        String destinatarioTipoDocumento,
        String destinatarioDocumento,
        String destinatarioNombre,
        BigDecimal pesoBrutoKg,
        Integer numeroBultos,
        String modalidad,
        String transportistaRuc,
        String transportistaRazonSocial,
        String vehiculoPlaca,
        String conductorDni,
        String conductorNombres,
        String conductorLicencia,
        String partidaUbigeo,
        String partidaDireccion,
        String llegadaUbigeo,
        String llegadaDireccion,
        String observaciones,
        DocumentoRelacionado relacionado,
        List<Linea> lineas) {

    public record Linea(String codigo, String descripcion, String unidad, BigDecimal cantidad) {
    }

    /* Comprobante de la venta que origina el traslado (01 factura, 03 boleta) */
    public record DocumentoRelacionado(String tipo, String numero) {
    }

    public String numero() {
        return serie + "-" + String.format("%08d", correlativo);
    }

    public String fileName(String rucEmisor) {
        return rucEmisor + "-09-" + numero();
    }
}
