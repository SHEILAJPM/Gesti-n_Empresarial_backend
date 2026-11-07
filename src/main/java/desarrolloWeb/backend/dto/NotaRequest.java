package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoNota;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/* Nota sobre una venta. En anulación (01), anulación por RUC (02) y devolución total (06) las líneas
   se copian de la venta y no hace falta enviarlas. fecha es opcional: por defecto, hoy. */
public record NotaRequest(
        @NotNull(message = "Indica la venta que se modifica") Long ventaId,
        @NotNull(message = "Indica si es nota de crédito o de débito") TipoNota tipo,
        @NotBlank(message = "Elige el motivo") String motivoCodigo,
        @NotBlank(message = "Explica el motivo (sustento)")
        @Size(max = 250, message = "El sustento no puede superar 250 caracteres") String sustento,
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
        @Valid @Size(max = 100, message = "Hasta 100 líneas") List<VentaRequest.Item> items) {
}
