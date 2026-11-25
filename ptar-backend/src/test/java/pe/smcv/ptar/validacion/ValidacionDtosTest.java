package pe.smcv.ptar.validacion;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import pe.smcv.ptar.parametro.dto.ParametroDto;
import pe.smcv.ptar.parametro.dto.UmbralDto;
import pe.smcv.ptar.reporte.dto.ReporteRequest;
import pe.smcv.ptar.reporte.entity.FormatoReporte;
import pe.smcv.ptar.reporte.entity.TipoReporte;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacionDtosTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void parametroRechazaRangoInvertido() {
        var request = new ParametroDto.Request("PH", "pH", null, "pH",
                new BigDecimal("9.000"), new BigDecimal("6.000"));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void parametroAceptaRangoOrdenado() {
        var request = new ParametroDto.Request("PH", "pH", null, "pH",
                new BigDecimal("0.000"), new BigDecimal("14.000"));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void umbralExigeAlMenosUnLimite() {
        var request = new UmbralDto.Request(1, null, null, null, null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void reporteRechazaFechasInvertidas() {
        var request = new ReporteRequest(TipoReporte.values()[0], FormatoReporte.values()[0],
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1), null, null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
