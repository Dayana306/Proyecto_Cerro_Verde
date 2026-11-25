package pe.smcv.ptar.notificacion.correo;

import java.time.LocalDateTime;

public record CorreoNotificacionResponse(
        Long idCorreo,
        String destinatarios,
        String asunto,
        String mensaje,
        boolean html,
        String nombresAdjuntos,
        String estado,
        LocalDateTime fechaEnvio,
        String errorResumen,
        String remitente) {

    public static CorreoNotificacionResponse desde(CorreoNotificacion correo) {
        String remitente = correo.getUsuario() == null ? "Sistema" : correo.getUsuario().getCorreo();
        return new CorreoNotificacionResponse(correo.getIdCorreo(), correo.getDestinatarios(), correo.getAsunto(),
                correo.getMensaje(), correo.isHtml(), correo.getNombresAdjuntos(), correo.getEstado(),
                correo.getFechaEnvio(), correo.getErrorResumen(), remitente);
    }
}
