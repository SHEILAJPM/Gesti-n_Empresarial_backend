package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.MovimientoStock.Motivo;
import desarrolloWeb.backend.entity.MovimientoStock.Tipo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class InventarioDtos {

    private InventarioDtos() {
    }

    public enum EstadoStock { SIN_STOCK, BAJO, NORMAL }

    /* AJUSTE: cada línea trae el conteo físico y se registra la diferencia */
    public enum Operacion { ENTRADA, SALIDA, AJUSTE }

    public record ProductoStock(Long id, String codigo, String descripcion, String unidad, boolean activo,
                                BigDecimal stock, BigDecimal stockMinimo, EstadoStock estado,
                                LocalDateTime ultimoMovimiento) {
    }

    public record Movimiento(Long id, LocalDateTime fecha, Tipo tipo, Motivo motivo, String motivoTexto,
                             BigDecimal cantidad, BigDecimal saldo, String referencia, String observacion,
                             String registradoPor, Long ventaId, Long compraId, Long productoId, String producto) {
    }

    public record Kardex(ProductoStock producto, BigDecimal entradas, BigDecimal salidas,
                         List<Movimiento> movimientos) {
    }

    public record Linea(
            @NotNull(message = "Elige el producto")
            Long productoId,
            @NotNull(message = "Indica la cantidad")
            @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
            @Digits(integer = 9, fraction = 3, message = "La cantidad admite hasta 9 enteros y 3 decimales")
            BigDecimal cantidad) {
    }

    public record MovimientoRequest(
            @NotNull(message = "Indica si es un ingreso, una salida o un ajuste")
            Operacion operacion,
            Motivo motivo,
            Long compraId,
            @Size(max = 80, message = "La referencia admite hasta 80 caracteres")
            String referencia,
            @Size(max = 200, message = "La observación admite hasta 200 caracteres")
            String observacion,
            @NotEmpty(message = "Agrega al menos un producto")
            @Size(max = 100, message = "Hasta 100 productos por movimiento")
            List<@Valid Linea> lineas) {
    }

    /* Compras recientes para vincular un ingreso; ingresada = ya tiene un ingreso registrado */
    public record CompraParaIngreso(Long id, String numeroComprobante, String proveedor, LocalDate fecha,
                                    boolean ingresada) {
    }

    /* Motivos que se registran a mano, con su texto */
    public record Motivos(Map<Motivo, String> entrada, Map<Motivo, String> salida) {
    }
}
