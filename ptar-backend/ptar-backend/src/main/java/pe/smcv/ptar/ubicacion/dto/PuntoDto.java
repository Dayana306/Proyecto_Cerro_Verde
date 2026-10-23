package pe.smcv.ptar.ubicacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PuntoDto {
    private PuntoDto() {}

    public record Request(
            @NotBlank @Size(max = 100) String nombrePunto,
            @Size(max = 200) String descripcion,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitud,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitud,
            @NotNull Integer idZona,
            @NotNull Integer idArea) {}

    public record Response(Integer idPunto, String nombrePunto, String descripcion,
                           BigDecimal latitud, BigDecimal longitud, Estado estado,
                           LocalDateTime fechaCreacion,
                           Integer idZona, String zona, Integer idArea, String area) {
        public static Response desde(PuntoMonitoreo p) {
            return new Response(p.getIdPunto(), p.getNombrePunto(), p.getDescripcion(),
                    p.getLatitud(), p.getLongitud(), p.getEstado(), p.getFechaCreacion(),
                    p.getZona().getIdZona(), p.getZona().getNombreZona(),
                    p.getArea().getIdArea(), p.getArea().getNombreArea());
        }
    }
}
