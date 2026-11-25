package pe.smcv.ptar.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilUpdateRequest(
        @NotBlank @Size(max=80) String nombre,
        @NotBlank @Size(max=80) String apellido,
        @Size(max=20) String telefono,
        @Size(max=2800000, message="La imagen es demasiado grande") String fotoPerfil) {
}
