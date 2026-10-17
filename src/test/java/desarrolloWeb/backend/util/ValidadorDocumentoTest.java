package desarrolloWeb.backend.util;

import desarrolloWeb.backend.entity.TipoComprobante;
import desarrolloWeb.backend.entity.TipoDocumento;
import desarrolloWeb.backend.exception.DocumentoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidadorDocumento")
class ValidadorDocumentoTest {

    private ValidadorDocumento validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorDocumento();
    }

    @Test
    @DisplayName("acepta RUC de 11 dígitos que empiezan con 10, 15, 17 o 20")
    void rucValidos() {
        assertAll(
                () -> assertTrue(validador.esRuc("10456789123")),
                () -> assertTrue(validador.esRuc("15123456789")),
                () -> assertTrue(validador.esRuc("17123456789")),
                () -> assertTrue(validador.esRuc("20123456789"))
        );
    }

    @Test
    @DisplayName("rechaza RUC con prefijo, longitud o caracteres inválidos")
    void rucInvalidos() {
        assertAll(
                () -> assertFalse(validador.esRuc("30123456789")),
                () -> assertFalse(validador.esRuc("2012345678")),
                () -> assertFalse(validador.esRuc("20A23456789")),
                () -> assertFalse(validador.esRuc(null))
        );
    }

    @Test
    @DisplayName("acepta DNI de 8 dígitos y rechaza el resto")
    void dni() {
        assertAll(
                () -> assertTrue(validador.esDni("10203040")),
                () -> assertFalse(validador.esDni("1020304")),
                () -> assertFalse(validador.esDni("1020304A")),
                () -> assertFalse(validador.esDni(null))
        );
    }

    @Test
    @DisplayName("un RUC exige FACTURA y un DNI exige BOLETA")
    void tipoComprobantePorDocumento() {
        assertAll(
                () -> assertEquals(TipoComprobante.FACTURA, validador.tipoComprobantePara("20123456789")),
                () -> assertEquals(TipoComprobante.BOLETA, validador.tipoComprobantePara("10203040"))
        );
    }

    @Test
    @DisplayName("deduce el tipo de documento DNI o RUC")
    void tipoDocumento() {
        assertAll(
                () -> assertEquals(TipoDocumento.RUC, validador.tipoDocumentoDe("20123456789")),
                () -> assertEquals(TipoDocumento.DNI, validador.tipoDocumentoDe("10203040"))
        );
    }

    @Test
    @DisplayName("un documento que no es DNI ni RUC lanza DocumentoInvalidoException")
    void documentoInvalidoLanzaExcepcion() {
        DocumentoInvalidoException error = assertThrows(DocumentoInvalidoException.class,
                () -> validador.tipoComprobantePara("12345"));

        assertEquals("El documento 12345 no es un DNI (8 dígitos) ni un RUC válido "
                + "(11 dígitos que empieza con 10, 15, 17 o 20)", error.getMessage());
    }
}
