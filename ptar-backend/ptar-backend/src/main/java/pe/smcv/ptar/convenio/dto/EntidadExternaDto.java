package pe.smcv.ptar.convenio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.entity.EntidadExterna;
import pe.smcv.ptar.convenio.entity.TipoEntidad;

import java.time.LocalDateTime;

public final class EntidadExternaDto {
    private EntidadExternaDto() {}

    public record Request(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull TipoEntidad tipoEntidad,
            @Email @Size(max = 120) String correo,
            @Size(max = 20) String telefono) {}

    public record Response(Integer idEntidad, String nombre, TipoEntidad tipoEntidad, String correo,
                           String telefono, Estado estado, LocalDateTime fechaCreacion) {
        public static Response desde(EntidadExterna e) {
            return new Response(e.getIdEntidad(), e.getNombre(), e.getTipoEntidad(), e.getCorreo(),
                    e.getTelefono(), e.getEstado(), e.getFechaCreacion());
        }
    }
}
