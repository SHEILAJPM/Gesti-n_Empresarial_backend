package desarrolloWeb.backend.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/* Motivos de traslado más usados (catálogo 20 de SUNAT) */
public final class MotivoTraslado {

    public static final Map<String, String> CATALOGO;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("01", "Venta");
        m.put("14", "Venta sujeta a confirmación del comprador");
        m.put("02", "Compra");
        m.put("04", "Traslado entre establecimientos de la misma empresa");
        m.put("18", "Traslado emisor itinerante de comprobantes de pago");
        m.put("13", "Otros");
        CATALOGO = Collections.unmodifiableMap(m);
    }

    private MotivoTraslado() {
    }

    public static String validar(String codigo) {
        if (codigo == null || !CATALOGO.containsKey(codigo)) {
            throw new IllegalArgumentException("Motivo de traslado no válido: " + codigo);
        }
        return codigo;
    }
}
