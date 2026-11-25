package pe.smcv.ptar.convenio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.entity.Convenio;
import pe.smcv.ptar.convenio.entity.TipoMeta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ConvenioDto {
    private ConvenioDto() {}

    public record Request(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull @Positive Integer idEntidad,
            @Positive Integer idPunto,
            @NotNull LocalDate fechaInicio,
            LocalDate fechaFin,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 3, fraction = 3) BigDecimal caudalMetaM3s,
            TipoMeta tipoMeta,
            @Size(max = 255) String observacion) {
        @AssertTrue(message = "La fecha de fin debe ser igual o posterior a la fecha de inicio")
        public boolean isPeriodoValido() {
            return fechaFin == null || fechaInicio == null || !fechaFin.isBefore(fechaInicio);
        }
    }

    public record Response(Integer idConvenio, String nombre, Integer idEntidad, String entidad,
                           Integer idPunto, String punto, LocalDate fechaInicio, LocalDate fechaFin,
                           BigDecimal caudalMetaM3s, TipoMeta tipoMeta, String observacion, Estado estado) {
        public static Response desde(Convenio c) {
            return new Response(c.getIdConvenio(), c.getNombre(),
                    c.getEntidad().getIdEntidad(), c.getEntidad().getNombre(),
                    c.getPunto() == null ? null : c.getPunto().getIdPunto(),
                    c.getPunto() == null ? "Todos los puntos" : c.getPunto().getNombrePunto(),
                    c.getFechaInicio(), c.getFechaFin(), c.getCaudalMetaM3s(), c.getTipoMeta(),
                    c.getObservacion(), c.getEstado());
        }
    }
}
