package desarrolloWeb.backend.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/* Unidades de medida más usadas del catálogo 03 de SUNAT (UN/ECE rec 20) */
public final class UnidadMedida {

    public static final Map<String, String> CATALOGO;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("NIU", "Unidad");
        m.put("ZZ", "Servicio");
        m.put("KGM", "Kilogramo");
        m.put("GRM", "Gramo");
        m.put("LTR", "Litro");
        m.put("GLL", "Galón");
        m.put("MTR", "Metro");
        m.put("MTK", "Metro cuadrado");
        m.put("BX", "Caja");
        m.put("PK", "Paquete");
        m.put("DZN", "Docena");
        m.put("HUR", "Hora");
        // Se conserva el orden: es el de la lista desplegable del formulario
        CATALOGO = Collections.unmodifiableMap(m);
    }

    private UnidadMedida() {
    }

    public static String validar(String codigo) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        if (!CATALOGO.containsKey(c)) {
            throw new IllegalArgumentException("Unidad de medida no válida: " + codigo
                    + ". Usa una de: " + String.join(", ", CATALOGO.keySet()));
        }
        return c;
    }
}
