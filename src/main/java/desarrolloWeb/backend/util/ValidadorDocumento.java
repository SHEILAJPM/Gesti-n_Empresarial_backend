package desarrolloWeb.backend.util;

import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import java.util.regex.Pattern;

public class ValidadorDocumento {

    private static final Pattern RUC = Pattern.compile("^(10|15|17|20)\\d{9}$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");

    public boolean esRuc(String documento) {
        return documento != null && RUC.matcher(documento).matches();
    }

    public boolean esDni(String documento) {
        return documento != null && DNI.matcher(documento).matches();
    }

    public TipoDocumento tipoDocumentoDe(String documento) {
        if (esRuc(documento)) {
            return TipoDocumento.RUC;
        }
        if (esDni(documento)) {
            return TipoDocumento.DNI;
        }
        throw new DocumentoInvalidoException("El documento " + documento + " no es un DNI (8 dígitos) ni un RUC válido "
                + "(11 dígitos que empieza con 10, 15, 17 o 20)");
    }

    public TipoComprobante tipoComprobantePara(String documento) {
        return tipoDocumentoDe(documento) == TipoDocumento.RUC
                ? TipoComprobante.FACTURA
                : TipoComprobante.BOLETA;
    }
}
