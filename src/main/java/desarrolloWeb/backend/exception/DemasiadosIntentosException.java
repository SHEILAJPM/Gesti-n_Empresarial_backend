package desarrolloWeb.backend.exception;

/* Demasiados intentos fallidos de inicio de sesión: responde 429 */
public class DemasiadosIntentosException extends RuntimeException {
    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
