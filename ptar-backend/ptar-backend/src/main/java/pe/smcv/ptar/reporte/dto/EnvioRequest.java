package pe.smcv.ptar.reporte.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.reporte.entity.MedioEnvio;

import java.time.LocalDateTime;

/** fechaEnvio opcional (por defecto, ahora). */
public record EnvioRequest(
        @NotNull Integer idEntidad,
        @NotNull MedioEnvio medio,
        LocalDateTime fechaEnvio,
        @Size(max = 255) String observacion) {
}
