package pe.smcv.ptar.usuario.dto;

import pe.smcv.ptar.common.enums.Estado;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Integer idUsuario,
        String nombre,
        String apellido,
        String correo,
        String usuarioLogin,
        String telefono,
        Estado estado,
        Integer idRol,
        String rol,
        Integer idArea,
        LocalDateTime fechaCreacion) {
}
