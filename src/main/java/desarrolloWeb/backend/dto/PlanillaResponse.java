package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.EstadoPlanilla;
import desarrolloWeb.backend.entity.PlanillaDetalle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/* detalles y omitidos vienen vacíos en el listado; se llenan al consultar una planilla */
public record PlanillaResponse(
        Long id,
        Integer anio,
        Integer mes,
        EstadoPlanilla estado,
        LocalDateTime fechaGeneracion,
        String generadaPor,
        LocalDateTime fechaAprobacion,
        String aprobadaPor,
        LocalDate fechaPago,
        BigDecimal totalBruto,
        BigDecimal totalDescuentos,
        BigDecimal totalNeto,
        BigDecimal totalEssalud,
        int cantidadTrabajadores,
        int pagosRealizados,
        List<Detalle> detalles,
        List<String> omitidos) {

    public record Detalle(
            Long id,
            Long trabajadorId,
            String nombres,
            String documentoIdentidad,
            String cargo,
            String area,
            LocalDate fechaIngreso,
            BigDecimal sueldoBase,
            Integer diasLaborados,
            BigDecimal remuneracionBruta,
            String sistemaPension,
            String afp,
            BigDecimal tasaPension,
            BigDecimal descuentoPension,
            BigDecimal essalud,
            BigDecimal neto,
            Long pagoId,
            String estadoPago) {

        public static Detalle de(PlanillaDetalle d, String estadoPago) {
            return new Detalle(d.getId(), d.getTrabajadorId(), d.getNombres(), d.getDocumentoIdentidad(), d.getCargo(),
                    d.getArea(), d.getFechaIngreso(), d.getSueldoBase(), d.getDiasLaborados(), d.getRemuneracionBruta(),
                    d.getSistemaPension(), d.getAfp(), d.getTasaPension(), d.getDescuentoPension(), d.getEssalud(),
                    d.getNeto(), d.getPagoId(), estadoPago);
        }
    }
}
