package desarrolloWeb.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CalculadoraPlanilla")
class CalculadoraPlanillaTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    @Test
    void mesCompletoCuentaTreintaDias() {
        assertEquals(30, CalculadoraPlanilla.diasLaborados(LocalDate.of(2024, 2, 1), null, OCTUBRE));
    }

    @Test
    void ingresoAMitadDeMesSoloCuentaLosDiasTrabajados() {
        assertEquals(17, CalculadoraPlanilla.diasLaborados(LocalDate.of(2026, 10, 15), null, OCTUBRE));
    }

    @Test
    void ceseAMitadDeMesCuentaHastaElCese() {
        assertEquals(10, CalculadoraPlanilla.diasLaborados(LocalDate.of(2024, 2, 1), LocalDate.of(2026, 10, 10), OCTUBRE));
    }

    @Test
    void fueraDelPeriodoNoEntraEnPlanilla() {
        assertEquals(0, CalculadoraPlanilla.diasLaborados(LocalDate.of(2026, 11, 1), null, OCTUBRE));
        assertEquals(0, CalculadoraPlanilla.diasLaborados(LocalDate.of(2024, 2, 1), LocalDate.of(2026, 9, 30), OCTUBRE));
        assertEquals(0, CalculadoraPlanilla.diasLaborados(null, null, OCTUBRE));
    }

    @Test
    void onpDescuentaTrecepor100YEssaludEsNueve() {
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(new BigDecimal("2500.00"), 30, "ONP", null);

        assertEquals(new BigDecimal("2500.00"), c.remuneracionBruta());
        assertEquals(new BigDecimal("325.00"), c.descuentoPension());
        assertEquals(new BigDecimal("225.00"), c.essalud());
        assertEquals(new BigDecimal("2175.00"), c.neto());
    }

    @Test
    void afpSumaAporteSeguroYComisionDeSuAfp() {
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(new BigDecimal("2500.00"), 30, "AFP", "Integra");

        assertEquals(new BigDecimal("0.1292"), c.tasaPension());
        assertEquals(new BigDecimal("323.00"), c.descuentoPension());
        assertEquals(new BigDecimal("2177.00"), c.neto());
    }

    @Test
    void sueldoProporcionalALosDiasLaborados() {
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(new BigDecimal("3000.00"), 15, "ONP", null);

        assertEquals(new BigDecimal("1500.00"), c.remuneracionBruta());
        assertEquals(new BigDecimal("195.00"), c.descuentoPension());
    }

    @Test
    void sinSistemaDePensionesNoSePuedeCalcular() {
        assertThrows(IllegalArgumentException.class, () -> CalculadoraPlanilla.tasaPension(null, null));
        assertThrows(IllegalArgumentException.class, () -> CalculadoraPlanilla.tasaPension("AFP", "Desconocida"));
    }
}
