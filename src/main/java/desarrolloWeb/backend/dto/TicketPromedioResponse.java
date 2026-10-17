package desarrolloWeb.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TicketPromedioResponse(LocalDate desde, LocalDate hasta, BigDecimal ticketPromedio) {
}
