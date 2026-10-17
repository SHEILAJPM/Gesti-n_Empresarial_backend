package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.Desglose;
import desarrolloWeb.backend.dto.LiquidacionIgv;
import desarrolloWeb.backend.dto.ResumenTributario;
import desarrolloWeb.backend.dto.SimulacionResponse;
import desarrolloWeb.backend.entity.Compra;
import desarrolloWeb.backend.entity.EstadoVenta;
import desarrolloWeb.backend.entity.Venta;
import desarrolloWeb.backend.repository.CompraRepository;
import desarrolloWeb.backend.repository.VentaRepository;
import desarrolloWeb.backend.util.CalculadoraTributaria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class TributarioService {

    private final CompraRepository compraRepository;
    private final VentaRepository ventaRepository;
    private final ConfiguracionTributariaService configuracionService;
    private final CalculadoraTributaria calculadora = new CalculadoraTributaria();

    public TributarioService(CompraRepository compraRepository,
                             VentaRepository ventaRepository,
                             ConfiguracionTributariaService configuracionService) {
        this.compraRepository = compraRepository;
        this.ventaRepository = ventaRepository;
        this.configuracionService = configuracionService;
    }

    @Transactional(readOnly = true)
    public ResumenTributario generarResumen(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        BigDecimal totalComprasSinIgv = cero();
        BigDecimal igvCompras = cero();
        for (Compra compra : compraRepository.buscarPorPeriodo(desde, hasta)) {
            totalComprasSinIgv = totalComprasSinIgv.add(compra.getMontoSinIgv());
            igvCompras = igvCompras.add(compra.getIgv());
        }

        BigDecimal totalVentasSinIgv = cero();
        BigDecimal igvVentas = cero();
        for (Venta venta : ventaRepository.buscarPorEstadoYPeriodo(EstadoVenta.EMITIDA, desde, hasta)) {
            totalVentasSinIgv = totalVentasSinIgv.add(venta.getMontoSinIgv());
            igvVentas = igvVentas.add(venta.getIgv());
        }

        LiquidacionIgv liquidacion = calculadora.calcularIgvPorPagar(igvVentas, igvCompras);

        return new ResumenTributario(
                desde,
                hasta,
                totalComprasSinIgv,
                igvCompras,
                totalVentasSinIgv,
                igvVentas,
                liquidacion.igvPorPagar(),
                liquidacion.saldoAFavor(),
                compraRepository.contarPorPeriodo(desde, hasta),
                ventaRepository.contarPorEstadoYPeriodo(EstadoVenta.EMITIDA, desde, hasta));
    }

    @Transactional(readOnly = true)
    public ResumenTributario generarResumenMensual(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        YearMonth periodo = YearMonth.of(anio, mes);
        return generarResumen(periodo.atDay(1), periodo.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public SimulacionResponse simular(BigDecimal monto, boolean incluyeIgv) {
        BigDecimal tasa = configuracionService.obtenerTasaVigente(ConfiguracionTributariaService.CODIGO_IGV);

        if (incluyeIgv) {
            Desglose desglose = calculadora.desglosar(monto, tasa);
            return new SimulacionResponse(monto, true, tasa, desglose.base(), desglose.igv(),
                    monto.setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal base = monto.setScale(2, RoundingMode.HALF_UP);
        return new SimulacionResponse(monto, false, tasa, base,
                calculadora.calcularIgv(base, tasa), calculadora.calcularTotal(base, tasa));
    }

    private BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
