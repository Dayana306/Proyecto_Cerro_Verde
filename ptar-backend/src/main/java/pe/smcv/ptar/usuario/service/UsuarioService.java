package pe.smcv.ptar.usuario.service;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
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

//gestión de usuarios y roles
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Pageable pageable) {
        return PageResponse.from(usuarioRepository.findAll(pageable), UsuarioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer id) {
        return UsuarioMapper.toResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .filter(r -> r.getEstado() == Estado.ACTIVO)
                .map(UsuarioMapper::toResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest req) {
        String correo = req.correo().trim();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        if (usuarioRepository.existsByUsuarioLogin(req.usuarioLogin().trim())) {
            throw new ReglaNegocioException("Ya existe un usuario con ese nombre de usuario");
        }

        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setCorreo(correo);
        u.setUsuarioLogin(req.usuarioLogin().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setTelefono(req.telefono());
        u.setRol(buscarRolActivo(req.idRol()));
        u.setIdArea(req.idArea());
        return UsuarioMapper.toResponse(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioUpdateRequest req) {
        Usuario u = buscar(id);
        String correo = req.correo().trim();
        if (!u.getCorreo().equalsIgnoreCase(correo) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }

        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setCorreo(correo);
        u.setTelefono(req.telefono());
        u.setRol(buscarRolActivo(req.idRol()));
        u.setIdArea(req.idArea());
        return UsuarioMapper.toResponse(usuarioRepository.save(u));
    }

    //borrado logico los usuarios nunca se eliminan
    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, Estado nuevoEstado, Integer idUsuarioActual) {
        if (id.equals(idUsuarioActual) && nuevoEstado == Estado.INACTIVO) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta");
        }
        Usuario u = buscar(id);
        u.setEstado(nuevoEstado);
        return UsuarioMapper.toResponse(usuarioRepository.save(u));
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
}
