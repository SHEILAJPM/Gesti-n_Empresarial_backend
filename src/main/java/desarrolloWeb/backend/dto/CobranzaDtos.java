package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.MedioPago;
import desarrolloWeb.backend.util.CalculadoraCobranza;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/* Peticiones y respuestas de cuentas por cobrar */
public final class CobranzaDtos {

    private CobranzaDtos() {
    }

    /* Registrar un cobro. fecha es opcional: por defecto, hoy. */
    public record CobroRequest(
            @NotNull(message = "Indica la venta") Long ventaId,
            @NotNull(message = "Indica el monto")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales") BigDecimal monto,
            @PastOrPresent(message = "La fecha del cobro no puede ser futura") LocalDate fecha,
            @NotNull(message = "Indica el medio de pago") MedioPago medio,
            @Size(max = 60, message = "La referencia admite hasta 60 caracteres") String referencia) {
    }

    /* Estado de la cuenta (en este orden se listan): VENCIDA, AL_DIA (debe pero nada vencido) o PAGADA */
    public enum EstadoCuenta { VENCIDA, AL_DIA, PAGADA }

    /* Una factura al crédito en el listado */
    public record CuentaPorCobrar(Long ventaId, String numeroComprobante, LocalDate fecha, Long clienteId,
                                  String documentoCliente, String nombreCliente, BigDecimal deuda, BigDecimal cobrado,
                                  BigDecimal saldo, BigDecimal vencido, LocalDate proximoVencimiento,
                                  long diasAtraso, EstadoCuenta estado) {
    }

    public record CobroResponse(Long id, LocalDate fecha, BigDecimal monto, MedioPago medio, String referencia,
                                String registradoPor, LocalDateTime registradoEn) {
    }

    public record Tramo(String descripcion, LocalDate vence, BigDecimal monto, BigDecimal cobrado, BigDecimal pendiente,
                        CalculadoraCobranza.EstadoTramo estado, long diasVencido) {
    }

    /* Detalle de una factura: sus cuotas con lo aplicado y la lista de cobros */
    public record EstadoDeCuenta(CuentaPorCobrar cuenta, BigDecimal notasCredito, BigDecimal notasDebito,
                                 List<Tramo> tramos, List<CobroResponse> cobros) {
    }

    public record DeudaCliente(Long clienteId, String documento, String nombre, int facturas, BigDecimal saldo,
                               BigDecimal vencido) {
    }

    public record Resumen(BigDecimal porCobrar, BigDecimal vencido, BigDecimal cobradoEsteMes, int facturasPendientes,
                          int facturasVencidas, List<DeudaCliente> porCliente) {
    }
}
