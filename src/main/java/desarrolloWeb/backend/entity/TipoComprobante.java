package desarrolloWeb.backend.entity;

public enum TipoComprobante {

    FACTURA("F"),
    BOLETA("B");

    private final String prefijo;

    TipoComprobante(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
