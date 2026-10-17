package desarrolloWeb.backend.util;

import desarrolloWeb.backend.dto.Desglose;
import desarrolloWeb.backend.dto.LiquidacionIgv;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculadoraTributaria {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA);

    public BigDecimal calcularIgv(BigDecimal base, BigDecimal tasa) {
        validarMonto(base);
        validarTasa(tasa);
        return redondear(base.multiply(tasa));
    }

    public BigDecimal calcularTotal(BigDecimal base, BigDecimal tasa) {
        BigDecimal igv = calcularIgv(base, tasa);
        return redondear(base).add(igv);
    }

    public Desglose desglosar(BigDecimal precioConIgv, BigDecimal tasa) {
        validarMonto(precioConIgv);
        validarTasa(tasa);
        BigDecimal base = precioConIgv.divide(BigDecimal.ONE.add(tasa), ESCALA, REDONDEO);
        BigDecimal igv = redondear(precioConIgv).subtract(base);
        return new Desglose(base, igv);
    }

    public LiquidacionIgv calcularIgvPorPagar(BigDecimal debitoFiscal, BigDecimal creditoFiscal) {
        validarMonto(debitoFiscal);
        validarMonto(creditoFiscal);
        BigDecimal diferencia = redondear(debitoFiscal.subtract(creditoFiscal));
        return diferencia.signum() >= 0
                ? new LiquidacionIgv(diferencia, CERO)
                : new LiquidacionIgv(CERO, diferencia.negate());
    }

    private BigDecimal redondear(BigDecimal monto) {
        return monto.setScale(ESCALA, REDONDEO);
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
    }

    private void validarTasa(BigDecimal tasa) {
        if (tasa == null || tasa.signum() < 0 || tasa.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La tasa debe estar entre 0 y 1");
        }
    }
}
