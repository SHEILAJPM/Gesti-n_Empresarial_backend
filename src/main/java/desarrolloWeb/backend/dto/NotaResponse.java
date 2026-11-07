package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoNota;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NotaResponse(
        Long id,
        TipoNota tipo,
        String numero,
        LocalDate fecha,
        String motivoCodigo,
        String motivo,
        String sustento,
        Long ventaId,
        TipoComprobante tipoComprobanteAfectado,
        String numeroAfectado,
        String documentoCliente,
        String nombreCliente,
        BigDecimal montoSinIgv,
        BigDecimal igv,
        BigDecimal montoTotal,
        BigDecimal tasaAplicada,
        String registradaPor,
        ComprobanteSunatResponse sunat,
        List<VentaResponse.Item> items,
        String montoEnLetras) {
}
