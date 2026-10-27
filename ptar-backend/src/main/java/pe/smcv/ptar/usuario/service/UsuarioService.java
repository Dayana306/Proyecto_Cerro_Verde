package pe.smcv.ptar.usuario.service;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.enums.RolNombre;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.ubicacion.repository.AreaRepository;
import pe.smcv.ptar.usuario.dto.PerfilResponse;
import pe.smcv.ptar.usuario.dto.PerfilUpdateRequest;
import pe.smcv.ptar.usuario.dto.RolResponse;
import pe.smcv.ptar.usuario.dto.UsuarioCreateRequest;
import pe.smcv.ptar.usuario.dto.UsuarioResponse;
import pe.smcv.ptar.usuario.dto.UsuarioUpdateRequest;
import pe.smcv.ptar.usuario.entity.Rol;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.mapper.UsuarioMapper;
import pe.smcv.ptar.usuario.repository.RolRepository;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.util.List;

/** Gestión de usuarios y roles (RF-09 / HU-09). */
@Service
public class UsuarioService {

    private static final String ENTIDAD = "USUARIO";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          AreaRepository areaRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String q, Integer idRol, Estado estado, Pageable pageable) {
        String filtro = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.from(usuarioRepository.buscar(filtro, idRol, estado, pageable), UsuarioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer id) {
        return UsuarioMapper.toResponse(buscar(id));
    }

    /** Devuelve el perfil del usuario autenticado sin exponer su hash de contraseña. */
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Integer idUsuario) {
        return aPerfilResponse(buscar(idUsuario));
    }

    /** Actualiza únicamente los datos editables del perfil propio. */
    @Transactional
    public PerfilResponse actualizarPerfil(Integer idUsuario, PerfilUpdateRequest req) {
        Usuario usuario = buscar(idUsuario);
        usuario.setNombre(req.nombre().trim());
        usuario.setApellido(req.apellido().trim());

        String telefono = req.telefono();
        usuario.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());

        // Si no se envía fotoPerfil, conservar la actual. Una cadena vacía la elimina.
        if (req.fotoPerfil() != null) {
            String foto = req.fotoPerfil().trim();
            usuario.setFotoPerfil(foto.isEmpty() ? null : foto);
        }

        Usuario guardado = usuarioRepository.save(usuario);
        PerfilResponse respuesta = aPerfilResponse(guardado);
        // No registrar el contenido de la foto en auditoría (puede ser una cadena base64 grande).
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, idUsuario,
                "Perfil propio actualizado", null, null);
        return respuesta;
    }

    private PerfilResponse aPerfilResponse(Usuario u) {
        return new PerfilResponse(
                u.getIdUsuario(),
                u.getNombre(),
                u.getApellido(),
                u.getCorreo(),
                u.getUsuarioLogin(),
                u.getTelefono(),
                u.getRol().getNombre(),
                u.getFotoPerfil());
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .filter(r -> r.getEstado() == Estado.ACTIVO)
                .map(UsuarioMapper::toRolResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest req) {
        String correo = req.correo().trim().toLowerCase();
        String login = req.usuarioLogin().trim();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        if (usuarioRepository.existsByUsuarioLoginIgnoreCase(login)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese nombre de usuario");
        }

        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setCorreo(correo);
        u.setUsuarioLogin(login);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setTelefono(req.telefono());
        u.setRol(buscarRolActivo(req.idRol()));
        u.setIdArea(validarArea(req.idArea()));
        UsuarioResponse creado = UsuarioMapper.toResponse(usuarioRepository.save(u));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, creado.idUsuario(),
                "Usuario creado: " + creado.correo() + " (" + creado.rol() + ")", null, creado);
        return creado;
    }

    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest req, Integer idUsuarioActual) {
        Usuario u = buscar(id);
        UsuarioResponse antes = UsuarioMapper.toResponse(u);
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdUsuarioNot(correo, id)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        Rol nuevoRol = buscarRolActivo(req.idRol());
        if (id.equals(idUsuarioActual) && !nuevoRol.getNombre().equals(u.getRol().getNombre())) {
            throw new ReglaNegocioException("No puede cambiar su propio rol");
        }
        if (dejariaSinAdministradores(u, nuevoRol.getNombre(), u.getEstado())) {
            throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
        }

        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono());
        u.setRol(nuevoRol);
        u.setIdArea(validarArea(req.idArea()));
        UsuarioResponse despues = UsuarioMapper.toResponse(usuarioRepository.save(u));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Usuario modificado: " + despues.correo(),
                antes, despues);
        return despues;
    }

    /** Borrado lógico: los usuarios nunca se eliminan (trazabilidad, RF-10). */
    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, Estado nuevoEstado, Integer idUsuarioActual) {
        if (id.equals(idUsuarioActual) && nuevoEstado == Estado.INACTIVO) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta");
        }
        Usuario u = buscar(id);
        if (dejariaSinAdministradores(u, u.getRol().getNombre(), nuevoEstado)) {
            throw new ReglaNegocioException("Debe quedar al menos un administrador activo");
        }
        Estado anterior = u.getEstado();
        u.setEstado(nuevoEstado);
        UsuarioResponse r = UsuarioMapper.toResponse(usuarioRepository.save(u));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Usuario " + r.correo() + ": " + anterior + " -> " + nuevoEstado, null, null);
        return r;
    }

    /** El administrador asigna una nueva contraseña (p. ej. si el usuario la olvidó). */
    @Transactional
    public void restablecerPassword(Integer id, String passwordNueva) {
        Usuario u = buscar(id);
        u.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(u);
        auditoria.registrar(AccionAuditoria.CAMBIAR_PASSWORD, ENTIDAD, id,
                "Contraseña restablecida por administrador para " + u.getCorreo(), null, null);
    }

    /** El propio usuario cambia su contraseña. */
    @Transactional
    public void cambiarPasswordPropia(Integer idUsuario, String actual, String nueva) {
        Usuario u = buscar(idUsuario);
        if (!passwordEncoder.matches(actual, u.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta de la actual");
        }
        u.setPasswordHash(passwordEncoder.encode(nueva));
        usuarioRepository.save(u);
        auditoria.registrar(AccionAuditoria.CAMBIAR_PASSWORD, ENTIDAD, idUsuario,
                "Cambio de contraseña propia", null, null);
    }

    private boolean dejariaSinAdministradores(Usuario u, String rolNuevo, Estado estadoNuevo) {
        String admin = RolNombre.ADMINISTRADOR.name();
        boolean eraAdminActivo = admin.equals(u.getRol().getNombre()) && u.getEstado() == Estado.ACTIVO;
        boolean siguesiendoAdminActivo = admin.equals(rolNuevo) && estadoNuevo == Estado.ACTIVO;
        return eraAdminActivo && !siguesiendoAdminActivo
                && usuarioRepository.countByRol_NombreAndEstado(admin, Estado.ACTIVO) <= 1;
    }

    private Usuario buscar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
    }

    private Rol buscarRolActivo(Integer idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + idRol));
        if (rol.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("El rol seleccionado está inactivo");
        }
        return rol;
    }

    private Integer validarArea(Integer idArea) {
        if (idArea != null && !areaRepository.existsById(idArea)) {
            throw new NotFoundException("Área no encontrada: " + idArea);
        }
        return idArea;
    }
}
