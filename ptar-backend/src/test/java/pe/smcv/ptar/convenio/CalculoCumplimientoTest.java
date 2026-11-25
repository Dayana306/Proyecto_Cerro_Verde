package pe.smcv.ptar.convenio;

import org.junit.jupiter.api.Test;
import pe.smcv.ptar.convenio.service.CalculoCumplimiento;
import pe.smcv.ptar.convenio.service.CalculoCumplimiento.Medicion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculoCumplimientoTest {

    private static Medicion m(String fecha, String valor) {
        return new Medicion(LocalDateTime.parse(fecha), new BigDecimal(valor));
    }

    @Test
    void promedioDePromediosDiarios() {
        // Día 1: 1.0 y 2.0 -> 1.5 ; Día 2: 1.8 -> 1.8 ; promedio = 1.65 (no 1.6 que daría el promedio simple)
        CalculoCumplimiento.Resultado r = CalculoCumplimiento.calcular(List.of(
                m("2026-03-01T08:00", "1.0"), m("2026-03-01T16:00", "2.0"), m("2026-03-02T08:00", "1.8")));
        assertEquals(new BigDecimal("1.650"), r.promedio());
        assertEquals(2, r.diasConDatos());
        assertEquals(3, r.mediciones());
        assertEquals(1, r.meses().size());
        assertEquals(3, r.meses().get(0).mes());
    }

    @Test
    void sinDatosNoHayPromedio() {
        CalculoCumplimiento.Resultado r = CalculoCumplimiento.calcular(List.of());
        assertNull(r.promedio());
        assertTrue(r.meses().isEmpty());
        assertNull(CalculoCumplimiento.porcentaje(null, new BigDecimal("1.5")));
    }

    @Test
    void porcentajeDeLaMeta() {
        assertEquals(new BigDecimal("110.0"), CalculoCumplimiento.porcentaje(new BigDecimal("1.65"), new BigDecimal("1.5")));
    }
}
