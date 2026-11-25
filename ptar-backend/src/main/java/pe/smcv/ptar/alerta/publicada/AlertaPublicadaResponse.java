package pe.smcv.ptar.alerta.publicada;

import pe.smcv.ptar.alerta.entity.NivelAlerta;
import java.time.LocalDateTime;

public record AlertaPublicadaResponse(
        Integer idAlertaPublicada,
        String titulo,
        String descripcion,
        NivelAlerta nivel,
        String area,
        LocalDateTime fechaCreacion,
        String autor,
        boolean leida,
        LocalDateTime fechaLectura) {
    static AlertaPublicadaResponse desde(AlertaPublicada alerta, AlertaPublicadaLectura lectura) {
        return new AlertaPublicadaResponse(
                alerta.getIdAlertaPublicada(), alerta.getTitulo(), alerta.getDescripcion(),
                alerta.getNivel(), alerta.getArea(), alerta.getFechaCreacion(),
                alerta.getCreador().getNombre() + " " + alerta.getCreador().getApellido(),
                lectura != null, lectura == null ? null : lectura.getFechaLectura());
    }
}
