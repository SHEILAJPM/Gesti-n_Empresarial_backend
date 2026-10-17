package desarrolloWeb.backend.entity;

/* Estado del comprobante electrónico. PENDIENTE, ACEPTADO, RECHAZADO y EXCEPCION son los que devuelve el proveedor;
   NO_ENVIADO y ERROR son propios del sistema (todavía no se envió, o el envío falló antes de llegar al proveedor). */
public enum EstadoSunat {
    NO_ENVIADO,
    PENDIENTE,
    ACEPTADO,
    RECHAZADO,
    EXCEPCION,
    ERROR;

    public boolean esFinal() {
        return this == ACEPTADO || this == RECHAZADO;
    }
}
