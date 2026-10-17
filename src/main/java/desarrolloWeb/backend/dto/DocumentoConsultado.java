package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.TipoDocumento;

/* nombre: "Nombres Apellidos" (DNI) o razón social (RUC). direccion, estado y condicion solo llegan con RUC. */
public record DocumentoConsultado(
        String documento,
        TipoDocumento tipo,
        String nombre,
        String direccion,
        String estado,
        String condicion) {
}
