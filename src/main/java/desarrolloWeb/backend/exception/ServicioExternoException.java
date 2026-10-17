package desarrolloWeb.backend.exception;

/* Un servicio externo (consulta de DNI/RUC) no está configurado o no responde: 503 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }
}
