package pe.smcv.ptar.common.dto;

import jakarta.validation.constraints.NotNull;
import pe.smcv.ptar.common.enums.Estado;

/** Cuerpo de PATCH /{id}/estado. Desactivar = borrado lógico. */
public record EstadoRequest(@NotNull Estado estado) {
}
