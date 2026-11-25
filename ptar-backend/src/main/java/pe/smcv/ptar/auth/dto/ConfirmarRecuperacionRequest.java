package pe.smcv.ptar.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Confirmación de recuperación con código temporal enviado al correo. */
public record ConfirmarRecuperacionRequest(
        @NotBlank @Email @Size(max = 120) String correo,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "El código debe contener 8 dígitos") String codigo,
        @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String passwordNueva,
        @NotBlank @Size(min = 8, max = 72, message = "Confirma la contraseña con 8 a 72 caracteres") String confirmarPassword) {

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isConfirmacionValida() {
        return passwordNueva != null && passwordNueva.equals(confirmarPassword);
    }
}
