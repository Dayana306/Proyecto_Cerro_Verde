package pe.smcv.ptar.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//edición de datos 
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 80) String apellido,
        @NotBlank @Email @Size(max = 120) String correo,
        @Size(max = 20) String telefono,
        @NotNull Integer idRol,
        Integer idArea) {
}
