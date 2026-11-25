package pe.smcv.ptar.monitoreo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import pe.smcv.ptar.monitoreo.entity.TipoMuestreo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de un monitoreo con sus lecturas (CU02).
 * Ejemplo: {"idPunto":1,"tipoMuestreo":"MANUAL","horaMuestreo":"2026-10-22T08:00:00",
 *           "lecturas":[{"idParametro":1,"valor":7.2},{"idParametro":2,"valor":1.6}]}
 */
public record MonitoreoRequest(
        @NotNull @Positive Integer idPunto,
        @NotNull TipoMuestreo tipoMuestreo,
        @NotNull LocalDateTime horaMuestreo,
        @Size(max = 255) String observacion,
        @NotEmpty(message = "Debe registrar al menos una lectura") @Valid List<LecturaRequest> lecturas) {

    public record LecturaRequest(
            @NotNull @Positive Integer idParametro,
            @NotNull @Digits(integer = 7, fraction = 3) BigDecimal valor) {}
}
