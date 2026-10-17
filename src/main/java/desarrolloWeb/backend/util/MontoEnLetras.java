package desarrolloWeb.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/* Leyenda 1000 de SUNAT: el importe total en letras, p. ej. "SON CIENTO DIECIOCHO CON 00/100 SOLES" */
public final class MontoEnLetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO",
            "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO",
            "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS",
            "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private MontoEnLetras() {
    }

    public static String convertir(BigDecimal monto) {
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long enteros = redondeado.longValue();
        int centimos = redondeado.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String letras = enteros == 0 ? "CERO" : enLetras(enteros);
        return "SON " + letras + " CON " + String.format("%02d", centimos) + "/100 SOLES";
    }

    static String enLetras(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            String prefijo = millones == 1 ? "UN MILLON" : apocopar(enLetras(millones)) + " MILLONES";
            return n % 1_000_000 == 0 ? prefijo : prefijo + " " + enLetras(n % 1_000_000);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            String prefijo = miles == 1 ? "MIL" : apocopar(enLetras(miles)) + " MIL";
            return n % 1000 == 0 ? prefijo : prefijo + " " + enLetras(n % 1000);
        }
        if (n >= 100) {
            if (n == 100) {
                return "CIEN";
            }
            String centena = CENTENAS[(int) (n / 100)];
            return n % 100 == 0 ? centena : centena + " " + enLetras(n % 100);
        }
        if (n < 30) {
            return UNIDADES[(int) n];
        }
        String decena = DECENAS[(int) (n / 10)];
        return n % 10 == 0 ? decena : decena + " Y " + UNIDADES[(int) (n % 10)];
    }

    /* "VEINTIUNO MIL" -> "VEINTIUN MIL", "UNO" -> "UN" delante de MIL/MILLONES */
    private static String apocopar(String texto) {
        return texto.endsWith("UNO") ? texto.substring(0, texto.length() - 1) : texto;
    }
}
