package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Marcacion;
import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.entity.TipoMarcacion;

import java.util.List;

/* Lo que ve el trabajador al entrar a su portal */
public record MiPerfilResponse(
        String usuario,
        Rol rol,
        TrabajadorResponse trabajador,
        SedeResponse sede,
        TipoMarcacion siguienteMarcacion,
        List<Marcacion> marcacionesHoy) {
}
