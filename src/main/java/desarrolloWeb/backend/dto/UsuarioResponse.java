package desarrolloWeb.backend.dto;

import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.entity.Usuario;

public record UsuarioResponse(Long id, String username, Rol rol, boolean activo, Long trabajadorId, String trabajadorNombre) {

    public static UsuarioResponse de(Usuario usuario) {
        return de(usuario, null);
    }

    public static UsuarioResponse de(Usuario usuario, String trabajadorNombre) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.isActivo(),
                usuario.getTrabajadorId(), trabajadorNombre);
    }
}
