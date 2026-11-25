package pe.smcv.ptar.ubicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Mapa2d;

import java.time.LocalDateTime;

public final class Mapa2dDto {
    private Mapa2dDto() {}

    /** imagenRuta: URL pública de la imagen o ruta dentro del frontend (ej. /mapas/ptar.png). */
    public record Request(
            @NotBlank @Size(max = 100) String nombreMapa,
            @Size(max = 200) String descripcion,
            @NotBlank @Size(max = 255) String imagenRuta) {}

    public record Response(Integer idMapa, String nombreMapa, String descripcion, String imagenRuta,
                           Estado estado, LocalDateTime fechaActualizacion) {
        public static Response desde(Mapa2d m) {
            return new Response(m.getIdMapa(), m.getNombreMapa(), m.getDescripcion(), m.getImagenRuta(),
                    m.getEstado(), m.getFechaActualizacion());
        }
    }
}
