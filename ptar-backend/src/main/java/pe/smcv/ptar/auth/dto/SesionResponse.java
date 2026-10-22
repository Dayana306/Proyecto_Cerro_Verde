package pe.smcv.ptar.auth.dto;

//datos del usuario autenticado que no tenga información sensible
public record SesionResponse(
        Integer idUsuario,
        String nombreCompleto,
        String correo,
        String rol) {
}
