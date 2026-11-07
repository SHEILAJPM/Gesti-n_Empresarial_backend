package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/* Resumen mensual de asistencia por trabajador (vista de RRHH, exportable a CSV) */
public record ResumenAsistenciaResponse(int anio, int mes, LocalTime horaEntrada, int toleranciaMinutos,
                                        List<Fila> trabajadores) {

    public record Fila(Long trabajadorId, String nombres, String documento, String area,
                       int diasLaborables, int diasAsistidos, int faltas, int tardanzas, int minutosTardanza,
                       BigDecimal horasTrabajadas, int diasSinSalida) {
    }
}
