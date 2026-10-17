package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Trabajador;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrabajadorResponse(
        Long id,
        String nombres,
        String documentoIdentidad,
        String cargo,
        String area,
        String estado,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        LocalDate fechaIngreso,
        BigDecimal sueldo,
        String sistemaPension,
        String afp,
        String banco,
        String numeroCuenta,
        LocalDate fechaCese,
        String motivoCese) {

    public static TrabajadorResponse de(Trabajador t) {
        return new TrabajadorResponse(t.getId(), t.getNombres(), t.getDocumentoIdentidad(), t.getCargo(), t.getArea(),
                t.getEstado(), t.getFechaNacimiento(), t.getTelefono(), t.getCorreo(), t.getDireccion(),
                t.getFechaIngreso(), t.getSueldo(), t.getSistemaPension(), t.getAfp(), t.getBanco(),
                t.getNumeroCuenta(), t.getFechaCese(), t.getMotivoCese());
    }
}
