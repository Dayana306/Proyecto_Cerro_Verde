package pe.smcv.ptar.alerta.dto;

import pe.smcv.ptar.alerta.entity.CanalNotificacion;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.alerta.entity.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Integer idNotificacion,
        String mensaje,
        CanalNotificacion canal,
        LocalDateTime fechaEnvio,
        LocalDateTime fechaLectura,
        boolean leida,
        Integer idAlerta,
        NivelAlerta nivelAlerta,
        EstadoAlerta estadoAlerta) {

    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(n.getIdNotificacion(), n.getMensaje(), n.getCanal(), n.getFechaEnvio(),
                n.getFechaLectura(), n.getFechaLectura() != null,
                n.getAlerta().getIdAlerta(), n.getAlerta().getNivel(), n.getAlerta().getEstado());
    }
}
