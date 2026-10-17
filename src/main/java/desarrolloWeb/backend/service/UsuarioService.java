package desarrolloWeb.backend.service;

import desarrolloWeb.backend.dto.UsuarioRequest;
import desarrolloWeb.backend.dto.UsuarioResponse;
import desarrolloWeb.backend.entity.Rol;
import desarrolloWeb.backend.entity.Trabajador;
import desarrolloWeb.backend.entity.Usuario;
import desarrolloWeb.backend.exception.RecursoNoEncontradoException;
import desarrolloWeb.backend.exception.ReglaNegocioException;
import desarrolloWeb.backend.repository.TrabajadorRepository;
import desarrolloWeb.backend.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrabajadorRepository trabajadorRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          TrabajadorRepository trabajadorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.trabajadorRepository = trabajadorRepository;
    }

    /* Spring Security busca aquí al usuario en cada petición con Basic Auth */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        Map<Long, String> nombres = trabajadorRepository.findAll().stream()
                .collect(Collectors.toMap(Trabajador::getId, Trabajador::getNombres));
        return usuarioRepository.findAll().stream()
                .map(u -> UsuarioResponse.de(u, u.getTrabajadorId() == null ? null : nombres.get(u.getTrabajadorId())))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ReglaNegocioException("Ya existe el usuario " + request.username());
        }

        Trabajador trabajador = validarTrabajador(request.rol(), request.trabajadorId(), null);

        Usuario usuario = new Usuario(request.username(),
                passwordEncoder.encode(request.password()), request.rol());
        usuario.setTrabajadorId(request.trabajadorId());
        return UsuarioResponse.de(usuarioRepository.save(usuario), trabajador == null ? null : trabajador.getNombres());
    }

    /* Vincula (o desvincula con null) la ficha del trabajador a una cuenta existente */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioResponse vincularTrabajador(Long id, Long trabajadorId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
        Trabajador trabajador = validarTrabajador(usuario.getRol(), trabajadorId, usuario.getId());
        usuario.setTrabajadorId(trabajadorId);
        return UsuarioResponse.de(usuario, trabajador == null ? null : trabajador.getNombres());
    }

    private Trabajador validarTrabajador(Rol rol, Long trabajadorId, Long usuarioActualId) {
        if (trabajadorId == null) {
            if (rol.esPersonal()) {
                throw new ReglaNegocioException("El rol " + rol + " necesita estar vinculado a la ficha de un trabajador");
            }
            return null;
        }
        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el trabajador con id " + trabajadorId));
        if (!trabajador.estaActivo()) {
            throw new ReglaNegocioException(trabajador.getNombres() + " está dado de baja");
        }
        usuarioRepository.findAll().stream()
                .filter(u -> trabajadorId.equals(u.getTrabajadorId()) && !u.getId().equals(usuarioActualId))
                .findFirst()
                .ifPresent(u -> {
                    throw new ReglaNegocioException(trabajador.getNombres() + " ya tiene la cuenta " + u.getUsername());
                });
        return trabajador;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, String usuarioActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));

        if (!activo && usuario.getUsername().equals(usuarioActual)) {
            throw new ReglaNegocioException("No puedes desactivar tu propio usuario");
        }

        usuario.setActivo(activo);
        return UsuarioResponse.de(usuario);
    }

    /* Id del trabajador vinculado a la cuenta, o null */
    @Transactional(readOnly = true)
    public Long trabajadorDe(String username) {
        return usuarioRepository.findByUsername(username).map(Usuario::getTrabajadorId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean existe(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    /* Crea un usuario solo si todavía no existe (usuarios iniciales) */
    @Transactional
    public void crearSiNoExiste(String username, String password, Rol rol) {
        if (!usuarioRepository.existsByUsername(username)) {
            usuarioRepository.save(new Usuario(username, passwordEncoder.encode(password), rol));
        }
    }
}
