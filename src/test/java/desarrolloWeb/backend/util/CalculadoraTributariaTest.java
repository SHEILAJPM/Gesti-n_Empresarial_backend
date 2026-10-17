package desarrolloWeb.backend.util;

import desarrolloWeb.backend.dto.Desglose;
import desarrolloWeb.backend.dto.LiquidacionIgv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CalculadoraTributaria")
class CalculadoraTributariaTest {

    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private CalculadoraTributaria calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraTributaria();
    }

    @Test
    @DisplayName("calcula el IGV de S/ 100.00 al 18 % como S/ 18.00")
    void calcularIgvDeCien() {
        BigDecimal base = new BigDecimal("100.00");

        BigDecimal igv = calculadora.calcularIgv(base, TASA_IGV);

        assertEquals(new BigDecimal("18.00"), igv);
    }

    @Test
    @DisplayName("calcula el total de S/ 300.00 + IGV como S/ 354.00")
    void calcularTotalDeTrescientos() {
        BigDecimal base = new BigDecimal("300.00");

        BigDecimal total = calculadora.calcularTotal(base, TASA_IGV);

        assertEquals(new BigDecimal("354.00"), total);
    }

    @Test
    @DisplayName("redondea HALF_UP a 2 decimales: 0.125 pasa a 0.13")
    void redondeaHalfUp() {
        BigDecimal base = new BigDecimal("1.25");
        BigDecimal tasa = new BigDecimal("0.10");

        BigDecimal igv = calculadora.calcularIgv(base, tasa);

        assertEquals(new BigDecimal("0.13"), igv);
    }

    @Test
    @DisplayName("desglosa S/ 118.00 con IGV en base 100.00 + IGV 18.00")
    void desglosarPrecioConIgv() {
        BigDecimal precioConIgv = new BigDecimal("118.00");

        Desglose desglose = calculadora.desglosar(precioConIgv, TASA_IGV);

        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), desglose.base()),
                () -> assertEquals(new BigDecimal("18.00"), desglose.igv()),
                () -> assertEquals(precioConIgv, desglose.base().add(desglose.igv()))
        );
    }

    @Test
    @DisplayName("débito 54.00 y crédito 18.00 dejan IGV por pagar 36.00 y saldo a favor 0.00")
    void igvPorPagarPositivo() {
        LiquidacionIgv liquidacion = calculadora.calcularIgvPorPagar(
                new BigDecimal("54.00"), new BigDecimal("18.00"));

        assertAll(
                () -> assertEquals(new BigDecimal("36.00"), liquidacion.igvPorPagar()),
                () -> assertEquals(new BigDecimal("0.00"), liquidacion.saldoAFavor())
        );
    }

    @Test
    @DisplayName("si el crédito supera al débito, el exceso es saldo a favor y nada es negativo")
    void saldoAFavorCuandoCreditoSuperaDebito() {
        LiquidacionIgv liquidacion = calculadora.calcularIgvPorPagar(
                new BigDecimal("18.00"), new BigDecimal("54.00"));

        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), liquidacion.igvPorPagar()),
                () -> assertEquals(new BigDecimal("36.00"), liquidacion.saldoAFavor())
        );
    }

    @Test
    @DisplayName("un monto null lanza IllegalArgumentException con mensaje en español")
    void montoNullLanzaExcepcion() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> calculadora.calcularIgv(null, TASA_IGV));

        assertEquals("El monto es obligatorio", error.getMessage());
    }

    @Test
    @DisplayName("un monto negativo lanza IllegalArgumentException con mensaje en español")
    void montoNegativoLanzaExcepcion() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> calculadora.calcularTotal(new BigDecimal("-1.00"), TASA_IGV));

        assertEquals("El monto no puede ser negativo", error.getMessage());
    }

    @Test
    @DisplayName("una tasa fuera de 0 a 1 lanza IllegalArgumentException")
    void tasaInvalidaLanzaExcepcion() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> calculadora.calcularIgv(new BigDecimal("100.00"), new BigDecimal("18")));

        assertEquals("La tasa debe estar entre 0 y 1", error.getMessage());
    }
}
