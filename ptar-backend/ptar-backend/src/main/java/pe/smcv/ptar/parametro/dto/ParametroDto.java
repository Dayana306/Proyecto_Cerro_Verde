package pe.smcv.ptar.parametro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.entity.Parametro;

import java.math.BigDecimal;

public final class ParametroDto {
    private ParametroDto() {}

    public record Request(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_]+", message = "Solo letras, números y _")
            String codigo,
            @NotBlank @Size(max = 60) String nombreParametro,
            @Size(max = 200) String descripcion,
            @NotBlank @Size(max = 20) String unidadMedida,
            @NotNull BigDecimal valorMinValido,
            @NotNull BigDecimal valorMaxValido) {}

    public record Response(Integer idParametro, String codigo, String nombreParametro, String descripcion,
                           String unidadMedida, BigDecimal valorMinValido, BigDecimal valorMaxValido,
                           Estado estado) {
        public static Response desde(Parametro p) {
            return new Response(p.getIdParametro(), p.getCodigo(), p.getNombreParametro(), p.getDescripcion(),
                    p.getUnidadMedida(), p.getValorMinValido(), p.getValorMaxValido(), p.getEstado());
        }
    }
}
