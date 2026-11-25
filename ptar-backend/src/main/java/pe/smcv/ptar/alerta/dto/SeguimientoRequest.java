package pe.smcv.ptar.alerta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;

/** nuevoEstado opcional: si se omite, solo se agrega el comentario. */
public record SeguimientoRequest(
        @NotBlank @Size(max = 500) String comentario,
        EstadoAlerta nuevoEstado) {
}
