package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.FormaPago;
import desarrolloWeb.backend.entity.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* items y cuotas vienen vacíos en el listado; se llenan al consultar una venta (GET /api/ventas/{id}) */
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
        ComprobanteSunatResponse sunat,
        FormaPago formaPago,
        String observaciones,
        int cantidadItems,
        List<Item> items,
        List<Cuota> cuotas,
        // «SON: MIL CIENTO OCHENTA CON 00/100 SOLES», como se imprime en el comprobante
        String montoEnLetras) {

    public record Item(int orden, Long productoId, String codigo, String descripcion, String unidad,
                       BigDecimal cantidad, BigDecimal valorUnitario, BigDecimal valorVenta, BigDecimal igv) {
    }

    public record Cuota(int numero, LocalDate fechaVencimiento, BigDecimal monto) {
    }

    public VentaResponse(Long id, Long clienteId, String documentoCliente, String nombreCliente,
                         TipoComprobante tipoComprobante, String numeroComprobante, LocalDate fecha,
                         BigDecimal montoSinIgv, BigDecimal igv, BigDecimal montoTotal, BigDecimal tasaAplicada,
                         EstadoVenta estado, ComprobanteSunatResponse sunat) {
        this(id, clienteId, documentoCliente, nombreCliente, tipoComprobante, numeroComprobante, fecha, montoSinIgv,
                igv, montoTotal, tasaAplicada, estado, sunat, FormaPago.CONTADO, null, 0, List.of(), List.of(), null);
    }

    /* Para reportes y pruebas donde el comprobante electrónico no importa */
    public VentaResponse(Long id, Long clienteId, String documentoCliente, String nombreCliente,
                         TipoComprobante tipoComprobante, String numeroComprobante, LocalDate fecha,
                         BigDecimal montoSinIgv, BigDecimal igv, BigDecimal montoTotal, BigDecimal tasaAplicada,
                         EstadoVenta estado) {
        this(id, clienteId, documentoCliente, nombreCliente, tipoComprobante, numeroComprobante, fecha, montoSinIgv,
                igv, montoTotal, tasaAplicada, estado, ComprobanteSunatResponse.noEnviado());
    }
}
