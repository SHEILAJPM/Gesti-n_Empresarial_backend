package desarrolloWeb.backend.entity;

/* CREDITO resta al comprobante (devoluciones, descuentos, anulación); DEBITO le suma (intereses, penalidades) */
public enum TipoNota {
    CREDITO("07", "C"),
    DEBITO("08", "D");

    private final String codigoSunat;
    private final String letraSerie;

    TipoNota(String codigoSunat, String letraSerie) {
        this.codigoSunat = codigoSunat;
        this.letraSerie = letraSerie;
    }

    public String getCodigoSunat() {
        return codigoSunat;
    }

    /* FC01 / BC01 / FD01 / BD01 */
    public String serie(TipoComprobante afectado) {
        return afectado.getPrefijo() + letraSerie + "01";
    }
}
