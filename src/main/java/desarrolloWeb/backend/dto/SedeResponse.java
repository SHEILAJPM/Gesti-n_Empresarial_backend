package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Sede;

import java.math.BigDecimal;

public record SedeResponse(Long id, String nombre, String direccion, BigDecimal latitud, BigDecimal longitud,
                           Integer radioMetros, Integer precisionMaxima) {

    public static SedeResponse de(Sede s) {
        return new SedeResponse(s.getId(), s.getNombre(), s.getDireccion(), s.getLatitud(), s.getLongitud(),
                s.getRadioMetros(), s.getPrecisionMaxima());
    }
}
