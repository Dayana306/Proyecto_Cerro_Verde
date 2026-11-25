package pe.smcv.ptar.parametro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.entity.Umbral;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class UmbralDto {
    private UmbralDto() {}

    /** idPunto null = umbral general del parámetro. Al menos un límite es obligatorio. */
    public record Request(
            @NotNull @Positive Integer idParametro,
            @Positive Integer idPunto,
            @Digits(integer = 7, fraction = 3) BigDecimal limiteMin,
            @Digits(integer = 7, fraction = 3) BigDecimal limiteMax,
            @Digits(integer = 7, fraction = 3) BigDecimal criticoMin,
            @Digits(integer = 7, fraction = 3) BigDecimal criticoMax) {
        @AssertTrue(message = "Los límites del umbral son obligatorios y deben mantener rangos coherentes")
        public boolean isRangoValido() {
            return pe.smcv.ptar.parametro.service.EvaluadorUmbral.validar(limiteMin, limiteMax, criticoMin, criticoMax) == null;
        }
    }

    /** Para PUT: parámetro y punto no cambian; solo los límites. */
    public record LimitesRequest(
            @Digits(integer = 7, fraction = 3) BigDecimal limiteMin,
            @Digits(integer = 7, fraction = 3) BigDecimal limiteMax,
            @Digits(integer = 7, fraction = 3) BigDecimal criticoMin,
            @Digits(integer = 7, fraction = 3) BigDecimal criticoMax) {
        @AssertTrue(message = "Los límites del umbral son obligatorios y deben mantener rangos coherentes")
        public boolean isRangoValido() {
            return pe.smcv.ptar.parametro.service.EvaluadorUmbral.validar(limiteMin, limiteMax, criticoMin, criticoMax) == null;
        }
    }

    public record Response(Integer idUmbral, Integer idParametro, String parametro, String unidadMedida,
                           Integer idPunto, String punto, BigDecimal limiteMin, BigDecimal limiteMax,
                           BigDecimal criticoMin, BigDecimal criticoMax, Estado estado,
                           LocalDateTime fechaCreacion, String configuradoPor) {
        public static Response desde(Umbral u) {
            return new Response(u.getIdUmbral(),
                    u.getParametro().getIdParametro(), u.getParametro().getNombreParametro(),
                    u.getParametro().getUnidadMedida(),
                    u.getPunto() == null ? null : u.getPunto().getIdPunto(),
                    u.getPunto() == null ? "General (todos los puntos)" : u.getPunto().getNombrePunto(),
                    u.getLimiteMin(), u.getLimiteMax(), u.getCriticoMin(), u.getCriticoMax(),
                    u.getEstado(), u.getFechaCreacion(), u.getUsuario().getNombreCompleto());
        }
    }
}
