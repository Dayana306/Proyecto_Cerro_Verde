package pe.smcv.ptar.reporte;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import pe.smcv.ptar.reporte.entity.TipoReporte;
import pe.smcv.ptar.reporte.service.ReporteDatos;
import pe.smcv.ptar.reporte.service.ReportePowerPointGenerator;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportePowerPointGeneratorTest {

    @Test
    void generaPresentacionPowerPointValidaConPortadaResumenYLecturas() throws Exception {
        ReporteDatos datos = new ReporteDatos(
                "Reporte ambiental de monitoreo hídrico", TipoReporte.AMBIENTAL,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "Todas las estaciones",
                "Prueba de generación", "Usuario de prueba", LocalDateTime.of(2026, 2, 1, 10, 0),
                List.of(new ReporteDatos.FilaResumen(1, "Estación 1", 1, "pH", "unidades pH", 2,
                        new BigDecimal("7.200"), new BigDecimal("7.100"), new BigDecimal("7.300"), 0)),
                List.of(new ReporteDatos.FilaLectura(LocalDateTime.of(2026, 1, 10, 8, 0), "Estación 1",
                        "pH", "unidades pH", new BigDecimal("7.200"))),
                List.of(), null, null);

        byte[] archivo = new ReportePowerPointGenerator().generar(datos);
        assertTrue(archivo.length > 4);
        assertEquals('P', archivo[0]);
        assertEquals('K', archivo[1]);
        try (XMLSlideShow presentacion = new XMLSlideShow(new ByteArrayInputStream(archivo))) {
            assertEquals(3, presentacion.getSlides().size());
        }
    }
}
