package pe.smcv.ptar.usuario.mapper;

import pe.smcv.ptar.usuario.dto.RolResponse;
import pe.smcv.ptar.usuario.dto.UsuarioResponse;
import pe.smcv.ptar.usuario.entity.Rol;
import pe.smcv.ptar.usuario.entity.Usuario;

//conversion entidad
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getNombre(),
                u.getApellido(),
                u.getCorreo(),
                u.getUsuarioLogin(),
                u.getTelefono(),
                u.getEstado(),
                u.getRol().getIdRol(),
                u.getRol().getNombre(),
                u.getIdArea(),
                u.getFechaCreacion());
    }

    public static RolResponse toResponse(Rol r) {
        return new RolResponse(r.getIdRol(), r.getNombre(), r.getDescripcion());
    }
}
