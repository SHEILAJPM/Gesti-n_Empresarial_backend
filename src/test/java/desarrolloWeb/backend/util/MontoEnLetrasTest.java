package desarrolloWeb.backend.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MontoEnLetrasTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0.50      | SON CERO CON 50/100 SOLES",
            "118.00    | SON CIENTO DIECIOCHO CON 00/100 SOLES",
            "100       | SON CIEN CON 00/100 SOLES",
            "354.00    | SON TRESCIENTOS CINCUENTA Y CUATRO CON 00/100 SOLES",
            "1475.00   | SON MIL CUATROCIENTOS SETENTA Y CINCO CON 00/100 SOLES",
            "21000.99  | SON VEINTIUN MIL CON 99/100 SOLES",
            "1000000   | SON UN MILLON CON 00/100 SOLES",
            "21500000  | SON VEINTIUN MILLONES QUINIENTOS MIL CON 00/100 SOLES"
    })
    void convierteElTotalALaLeyendaDeSunat(String monto, String esperado) {
        assertEquals(esperado, MontoEnLetras.convertir(new BigDecimal(monto)));
    }
}
