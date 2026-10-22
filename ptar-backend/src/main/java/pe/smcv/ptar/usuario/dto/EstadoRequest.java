package pe.smcv.ptar.usuario.dto;

import jakarta.validation.constraints.NotNull;
import pe.smcv.ptar.common.enums.Estado;

public record EstadoRequest(@NotNull Estado estado) {
}
