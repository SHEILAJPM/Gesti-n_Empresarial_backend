package desarrolloWeb.backend.entity;

/* Catálogo 18 de SUNAT: PUBLICO (01) contrata una empresa de transporte; PRIVADO (02) usa vehículo propio */
public enum ModalidadTraslado {
    PUBLICO("01"),
    PRIVADO("02");

    private final String codigoSunat;

    ModalidadTraslado(String codigoSunat) {
        this.codigoSunat = codigoSunat;
    }

    public String getCodigoSunat() {
        return codigoSunat;
    }
}
