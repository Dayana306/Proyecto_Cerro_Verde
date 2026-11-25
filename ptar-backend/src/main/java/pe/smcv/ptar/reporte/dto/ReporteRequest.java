package pe.smcv.ptar.reporte.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import java.time.temporal.ChronoUnit;
import pe.smcv.ptar.reporte.entity.FormatoReporte;
import pe.smcv.ptar.reporte.entity.TipoReporte;

import java.time.LocalDate;

/** idPunto null = todas las estaciones. Periodo máximo: 366 días. */
public record ReporteRequest(
        @NotNull TipoReporte tipoReporte,
        @NotNull FormatoReporte formato,
        @NotNull LocalDate fechaDesde,
        @NotNull LocalDate fechaHasta,
        @Positive Integer idPunto,
        @Size(max = 255) String descripcion) {
    @AssertTrue(message = "El rango debe ser válido y no puede superar 365 días")
    public boolean isPeriodoValido() {
        if (fechaDesde == null || fechaHasta == null || fechaDesde.isAfter(fechaHasta)) return false;
        return ChronoUnit.DAYS.between(fechaDesde, fechaHasta) < 366;
    }
}
