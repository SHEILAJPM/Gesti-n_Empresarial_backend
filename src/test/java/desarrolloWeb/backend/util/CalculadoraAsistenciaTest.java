package desarrolloWeb.backend.util;

import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.TipoMarcacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Resumen mensual de asistencia")
class CalculadoraAsistenciaTest {

    // Noviembre 2026: el 2 es lunes. Del 2 al 6 hay 5 días laborables.
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final LocalTime OCHO = LocalTime.of(8, 0);

    private static Marcacion m(int dia, int h, int min, TipoMarcacion tipo) {
        Marcacion m = new Marcacion();
        m.setTipo(tipo);
        m.setFechaHora(LocalDateTime.of(2026, 11, dia, h, min));
        return m;
    }

    @Test
    void cuentaHorasTardanzasFaltasYDiasSinSalida() {
        List<Marcacion> marcaciones = List.of(
                m(2, 7, 55, TipoMarcacion.ENTRADA), m(2, 17, 0, TipoMarcacion.SALIDA),   // 9 h 05, puntual
                m(3, 8, 25, TipoMarcacion.ENTRADA), m(3, 17, 25, TipoMarcacion.SALIDA),  // 9 h, 25 min tarde
                m(4, 8, 9, TipoMarcacion.ENTRADA));                                      // dentro de tolerancia, sin salida
        // El 5 es falta; el 6 es hoy y todavía no cuenta como falta
        CalculadoraAsistencia.Resultado r = CalculadoraAsistencia.calcular(marcaciones, NOV,
                LocalDate.of(2025, 1, 1), null, LocalDate.of(2026, 11, 6), OCHO, 10);

        assertEquals(5, r.diasLaborables());
        assertEquals(3, r.diasAsistidos());
        assertEquals(1, r.faltas());
        assertEquals(1, r.tardanzas());
        assertEquals(25, r.minutosTardanza());
        assertEquals(new BigDecimal("18.1"), r.horasTrabajadas());
        assertEquals(1, r.diasSinSalida());
    }

    @Test
    void soloCuentaDesdeElIngresoYHastaElCese() {
        CalculadoraAsistencia.Resultado r = CalculadoraAsistencia.calcular(List.of(), NOV,
                LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 30), OCHO, 10);

        assertEquals(2, r.diasLaborables());
        assertEquals(2, r.faltas());
    }

    @Test
    void finDeSemanaNoEsLaborableNiFalta() {
        CalculadoraAsistencia.Resultado r = CalculadoraAsistencia.calcular(List.of(), NOV,
                null, null, LocalDate.of(2026, 11, 9), OCHO, 10);

        // Del 2 al 6 (5 días) más el lunes 9 (hoy) = 6 laborables; faltas solo hasta ayer = 5
        assertEquals(6, r.diasLaborables());
        assertEquals(5, r.faltas());
    }
}
