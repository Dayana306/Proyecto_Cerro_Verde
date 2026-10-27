package pe.smcv.ptar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max=80) String nombre,
        @NotBlank @Size(max=80) String apellido,
        @NotBlank @Email @Size(max=120) String correo,
        @NotBlank @Size(min=3,max=40) String usuarioLogin,
        @NotBlank @Size(min=8,max=72) String password,
        @Size(max=20) String telefono) {
}
