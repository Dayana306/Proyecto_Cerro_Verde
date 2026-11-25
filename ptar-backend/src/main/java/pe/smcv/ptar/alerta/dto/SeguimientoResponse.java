package pe.smcv.ptar.alerta.dto;

import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.Seguimiento;

import java.time.LocalDateTime;

public record SeguimientoResponse(
        Integer idSeguimiento,
        String comentario,
        EstadoAlerta estadoAnterior,
        EstadoAlerta estadoNuevo,
        LocalDateTime fecha,
        Integer idUsuario,
        String usuario) {

    public static SeguimientoResponse desde(Seguimiento s) {
        return new SeguimientoResponse(s.getIdSeguimiento(), s.getComentario(), s.getEstadoAnterior(),
                s.getEstadoNuevo(), s.getFecha(), s.getUsuario().getIdUsuario(), s.getUsuario().getNombreCompleto());
    }
}
