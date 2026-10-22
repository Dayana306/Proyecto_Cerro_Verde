package pe.smcv.ptar.auth.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        SesionResponse usuario) {
}
