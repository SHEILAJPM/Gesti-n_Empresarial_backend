package desarrolloWeb.backend.dto;

/* trabajadorId null desvincula la cuenta (solo para roles que no son de personal) */
public record VincularTrabajadorRequest(Long trabajadorId) {
}
