package pe.smcv.ptar.reporte.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.reporte.entity.FormatoReporte;
import pe.smcv.ptar.reporte.entity.TipoReporte;

import java.time.LocalDate;

/** idPunto null = todas las estaciones. Periodo máximo: 366 días. */
public record ReporteRequest(
        @NotNull TipoReporte tipoReporte,
        @NotNull FormatoReporte formato,
        @NotNull LocalDate fechaDesde,
        @NotNull LocalDate fechaHasta,
        Integer idPunto,
        @Size(max = 255) String descripcion) {
}
