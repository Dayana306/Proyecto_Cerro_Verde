package pe.smcv.ptar.auth.dto;

/** Datos del usuario autenticado (sin información sensible). */
public record SesionResponse(
        Integer idUsuario,
        String nombreCompleto,
        String correo,
        String rol) {
}
