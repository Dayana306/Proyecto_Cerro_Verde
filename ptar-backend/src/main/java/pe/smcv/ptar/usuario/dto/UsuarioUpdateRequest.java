package pe.smcv.ptar.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de datos. La contraseña no se cambia por aquí. */
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 80) String apellido,
        @NotBlank @Email @Size(max = 120) String correo,
        @Size(max = 20) String telefono,
        @NotNull Integer idRol,
        Integer idArea) {
}
