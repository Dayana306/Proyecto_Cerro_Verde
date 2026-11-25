package pe.smcv.ptar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos públicos para crear una cuenta sin verificación por correo. */
public record RegistroRequest(
        @NotBlank(message = "Ingresa tus nombres") @Size(max = 80) String nombre,
        @NotBlank(message = "Ingresa tus apellidos") @Size(max = 80) String apellido,
        @NotBlank(message = "Ingresa tu correo") @Email(message = "Ingresa un correo electrónico válido") @Size(max = 120) String correo,
        @NotBlank(message = "Ingresa un nombre de usuario") @Size(min = 3, max = 40, message = "El usuario debe tener entre 3 y 40 caracteres") String usuarioLogin,
        @NotBlank(message = "Ingresa una contraseña") @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres") String password,
        @Pattern(regexp = "^9[0-9]{8}$", message = "El celular debe tener 9 dígitos y comenzar con 9") String telefono) {
}
