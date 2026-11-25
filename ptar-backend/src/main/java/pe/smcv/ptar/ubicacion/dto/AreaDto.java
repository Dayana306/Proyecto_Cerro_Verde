package pe.smcv.ptar.ubicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Area;

import java.time.LocalDateTime;

public final class AreaDto {
    private AreaDto() {}

    public record Request(
            @NotBlank @Size(max = 100) String nombreArea,
            @Size(max = 200) String descripcion) {}

    public record Response(Integer idArea, String nombreArea, String descripcion,
                           Estado estado, LocalDateTime fechaCreacion) {
        public static Response desde(Area a) {
            return new Response(a.getIdArea(), a.getNombreArea(), a.getDescripcion(),
                    a.getEstado(), a.getFechaCreacion());
        }
    }
}
