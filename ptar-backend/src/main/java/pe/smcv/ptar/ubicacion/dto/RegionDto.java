package pe.smcv.ptar.ubicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Region;

import java.time.LocalDateTime;

public final class RegionDto {
    private RegionDto() {}

    public record Request(
            @NotBlank @Size(max = 100) String nombreRegion,
            @Size(max = 200) String descripcion) {}

    public record Response(Integer idRegion, String nombreRegion, String descripcion,
                           Estado estado, LocalDateTime fechaCreacion) {
        public static Response desde(Region r) {
            return new Response(r.getIdRegion(), r.getNombreRegion(), r.getDescripcion(),
                    r.getEstado(), r.getFechaCreacion());
        }
    }
}
