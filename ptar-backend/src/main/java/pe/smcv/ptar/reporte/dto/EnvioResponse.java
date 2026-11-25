package pe.smcv.ptar.reporte.dto;

import pe.smcv.ptar.reporte.entity.MedioEnvio;
import pe.smcv.ptar.reporte.entity.ReporteEnvio;

import java.time.LocalDateTime;

public record EnvioResponse(
        Integer idEnvio,
        Integer idEntidad,
        String entidad,
        MedioEnvio medio,
        LocalDateTime fechaEnvio,
        String observacion,
        String registradoPor) {

    public static EnvioResponse desde(ReporteEnvio e) {
        return new EnvioResponse(e.getIdEnvio(), e.getEntidad().getIdEntidad(), e.getEntidad().getNombre(),
                e.getMedio(), e.getFechaEnvio(), e.getObservacion(), e.getUsuario().getNombreCompleto());
    }
}
