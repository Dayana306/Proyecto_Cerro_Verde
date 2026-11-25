package pe.smcv.ptar.alerta.publicada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.alerta.entity.NivelAlerta;

public record AlertaPublicadaRequest(
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotNull NivelAlerta nivel,
        @Size(max = 120) String area) {
}
