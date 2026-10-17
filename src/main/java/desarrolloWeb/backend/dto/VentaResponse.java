package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaResponse(
        Long id,
        Long clienteId,
        String documentoCliente,
        String nombreCliente,
        TipoComprobante tipoComprobante,
        String numeroComprobante,
        LocalDate fecha,
        BigDecimal montoSinIgv,
        BigDecimal igv,
        BigDecimal montoTotal,
        BigDecimal tasaAplicada,
        EstadoVenta estado,
        ComprobanteSunatResponse sunat) {

    /* Para reportes y pruebas donde el comprobante electrónico no importa */
    public VentaResponse(Long id, Long clienteId, String documentoCliente, String nombreCliente,
                         TipoComprobante tipoComprobante, String numeroComprobante, LocalDate fecha,
                         BigDecimal montoSinIgv, BigDecimal igv, BigDecimal montoTotal, BigDecimal tasaAplicada,
                         EstadoVenta estado) {
        this(id, clienteId, documentoCliente, nombreCliente, tipoComprobante, numeroComprobante, fecha, montoSinIgv,
                igv, montoTotal, tasaAplicada, estado, ComprobanteSunatResponse.noEnviado());
    }
}
