package pe.smcv.ptar.auditoria.dto;

import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.entity.Historial;

import java.time.LocalDateTime;

public record HistorialResponse(
        Long idHistorial,
        LocalDateTime fechaHora,
        AccionAuditoria accion,
        String entidad,
        Long idEntidad,
        String descripcion,
        String datosAnteriores,
        String datosNuevos,
        String ipOrigen,
        Integer idUsuario,
        String usuario) {

    public static HistorialResponse desde(Historial h) {
        return new HistorialResponse(h.getIdHistorial(), h.getFechaHora(), h.getAccion(), h.getEntidad(),
                h.getIdEntidad(), h.getDescripcion(), h.getDatosAnteriores(), h.getDatosNuevos(), h.getIpOrigen(),
                h.getUsuario() == null ? null : h.getUsuario().getIdUsuario(),
                h.getUsuario() == null ? "Sistema" : h.getUsuario().getNombreCompleto());
    }
}
