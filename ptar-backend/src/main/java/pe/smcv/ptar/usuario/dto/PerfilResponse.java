package pe.smcv.ptar.usuario.dto;

public record PerfilResponse(Integer idUsuario, String nombre, String apellido, String correo,
                             String usuarioLogin, String telefono, String rol, String fotoPerfil) {
}
