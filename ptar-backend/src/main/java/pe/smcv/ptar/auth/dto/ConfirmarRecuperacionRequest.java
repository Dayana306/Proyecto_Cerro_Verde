package pe.smcv.ptar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarRecuperacionRequest(
        @NotBlank @Size(min=32,max=128) String token,
        @NotBlank @Size(min=8,max=72,message="La contraseña debe tener entre 8 y 72 caracteres") String passwordNueva) {
}
