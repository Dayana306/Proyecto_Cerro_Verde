package pe.smcv.ptar.ubicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.TipoZona;
import pe.smcv.ptar.ubicacion.entity.Zona;

import java.time.LocalDateTime;

public final class ZonaDto {
    private ZonaDto() {}

    /** poligonoGeojson: texto GeoJSON (Polygon, MultiPolygon, Feature o FeatureCollection). Opcional. */
    public record Request(
            @NotBlank @Size(max = 100) String nombreZona,
            @Size(max = 200) String descripcion,
            @NotNull TipoZona tipoZona,
            String poligonoGeojson,
            @NotNull Integer idRegion) {}

    public record Response(Integer idZona, String nombreZona, String descripcion, TipoZona tipoZona,
                           String poligonoGeojson, Estado estado, LocalDateTime fechaCreacion,
                           Integer idRegion, String region) {
        public static Response desde(Zona z) {
            return new Response(z.getIdZona(), z.getNombreZona(), z.getDescripcion(), z.getTipoZona(),
                    z.getPoligonoGeojson(), z.getEstado(), z.getFechaCreacion(),
                    z.getRegion().getIdRegion(), z.getRegion().getNombreRegion());
        }
    }
}
