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

    private static final CalculadoraPlanilla.Parametros P = CalculadoraPlanilla.Parametros.REFERENCIALES;

    @Test
    void essaludNoBajaDeLaRemuneracionMinima() {
        CalculadoraPlanilla.Calculo completo = CalculadoraPlanilla.calcular(new BigDecimal("900.00"), 30, "ONP", null, P);
        assertEquals(new BigDecimal("101.70"), completo.essalud());

        // Con medio mes, la base mínima también es proporcional: 1130 × 15 / 30 = 565
        CalculadoraPlanilla.Calculo medio = CalculadoraPlanilla.calcular(new BigDecimal("900.00"), 15, "ONP", null, P);
        assertEquals(new BigDecimal("50.85"), medio.essalud());
    }

    @Test
    void primaDeSeguroAfpSeTopaEnLaRemuneracionMaximaAsegurable() {
        // 20000 × (10 % + 1.55 %) + 12234.34 × 1.37 % = 2310 + 167.61
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(new BigDecimal("20000.00"), 30, "AFP", "Integra", P);
        assertEquals(new BigDecimal("2477.61"), c.descuentoPension());
    }

    @Test
    void sueldoBajoNoTieneRetencionDeQuinta() {
        // 2500 × 14 = 35000, menos que 7 UIT (38500)
        assertEquals(new BigDecimal("0.00"), CalculadoraPlanilla.retencionQuinta(1, new BigDecimal("2500.00"),
                new BigDecimal("2500.00"), BigDecimal.ZERO, java.util.Map.of(), P));
    }

    @Test
    void quintaDeEneroEsElImpuestoAnualEntreDoce() {
        // Proyección 5000 × 14 = 70000; renta neta 31500 → 27500 × 8 % + 4000 × 14 % = 2760 → 2760 / 12
        assertEquals(new BigDecimal("230.00"), CalculadoraPlanilla.retencionQuinta(1, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), BigDecimal.ZERO, java.util.Map.of(), P));
    }

    @Test
    void quintaDeAbrilRestaLoRetenidoYDivideEntreNueve() {
        java.util.Map<Integer, BigDecimal> retenido = java.util.Map.of(1, new BigDecimal("230.00"),
                2, new BigDecimal("230.00"), 3, new BigDecimal("230.00"));
        assertEquals(new BigDecimal("230.00"), CalculadoraPlanilla.retencionQuinta(4, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), new BigDecimal("15000.00"), retenido, P));
    }

    @Test
    void escalaDeRentaAplicaLosCincoTramos() {
        // Neta 961500: 2200 + 11550 + 14025 + 11000 + 214200
        assertEquals(new BigDecimal("252975.00"),
                CalculadoraPlanilla.impuestoAnual(new BigDecimal("1000000"), new BigDecimal("5500")));
    }

    @Test
    void elNetoDescuentaPensionYQuinta() {
        CalculadoraPlanilla.Calculo c = CalculadoraPlanilla.calcular(new BigDecimal("5000.00"), 30, "ONP", null, P)
                .conRentaQuinta(new BigDecimal("230.00"));
        assertEquals(new BigDecimal("4120.00"), c.neto());
    }
}
