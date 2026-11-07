package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.ModalidadTraslado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GuiaResponse(
        Long id,
        String numero,
        LocalDate fechaEmision,
        LocalDate fechaTraslado,
        String motivoCodigo,
        String motivo,
        Long ventaId,
        String comprobanteRelacionado,
        String destinatarioTipoDocumento,
        String destinatarioDocumento,
        String destinatarioNombre,
        BigDecimal pesoBrutoKg,
        Integer numeroBultos,
        ModalidadTraslado modalidad,
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
        String registradaPor,
        ComprobanteSunatResponse sunat,
        int cantidadItems,
        List<Item> items) {

    public record Item(int orden, String codigo, String descripcion, String unidad, BigDecimal cantidad) {
    }
}
