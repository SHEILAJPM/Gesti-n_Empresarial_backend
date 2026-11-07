package desarrolloWeb.backend.util;

import desarrolloWeb.backend.entity.TipoNota;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/* Motivos de las notas: catálogo 09 (crédito) y 10 (débito) de SUNAT */
public final class MotivoNota {

    public static final Map<String, String> CREDITO;
    public static final Map<String, String> DEBITO;

    /* Estos motivos revierten todo el comprobante: la nota copia todas sus líneas por el saldo pendiente */
    public static final Set<String> TOTALES = Set.of("01", "02", "06");

    /* Notas de crédito que regresan mercadería al almacén (además de las totales) */
    public static final String DEVOLUCION_TOTAL = "06";
    public static final String DEVOLUCION_POR_ITEM = "07";

    static {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("01", "Anulación de la operación");
        c.put("02", "Anulación por error en el RUC");
        c.put("03", "Corrección por error en la descripción");
        c.put("04", "Descuento global");
        c.put("05", "Descuento por ítem");
        c.put("06", "Devolución total");
        c.put("07", "Devolución por ítem");
        c.put("08", "Bonificación");
        c.put("09", "Disminución en el valor");
        CREDITO = Collections.unmodifiableMap(c);

        Map<String, String> d = new LinkedHashMap<>();
        d.put("01", "Intereses por mora");
        d.put("02", "Aumento en el valor");
        d.put("03", "Penalidades / otros conceptos");
        DEBITO = Collections.unmodifiableMap(d);
    }

    private MotivoNota() {
    }

    public static Map<String, String> de(TipoNota tipo) {
        return tipo == TipoNota.CREDITO ? CREDITO : DEBITO;
    }

    public static String validar(TipoNota tipo, String codigo) {
        if (codigo == null || !de(tipo).containsKey(codigo)) {
            throw new IllegalArgumentException("Motivo no válido para una nota de "
                    + (tipo == TipoNota.CREDITO ? "crédito" : "débito") + ": " + codigo);
        }
        return codigo;
    }

    public static boolean esTotal(TipoNota tipo, String codigo) {
        return tipo == TipoNota.CREDITO && TOTALES.contains(codigo);
    }

    public static boolean esDevolucionPorItem(TipoNota tipo, String codigo) {
        return tipo == TipoNota.CREDITO && DEVOLUCION_POR_ITEM.equals(codigo);
    }
}
