package pe.smcv.ptar.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El administrador asigna una contraseña nueva a otro usuario. */
public record PasswordResetRequest(
        @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String passwordNueva) {
}
