package pe.smcv.ptar.reporte.service;

import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Presentación PowerPoint con portada y secciones de resumen, lecturas, alertas y bitácora. */
@Component
public class ReportePowerPointGenerator {

    private static final Color VERDE = new Color(23, 61, 53);
    private static final Color TEXTO = new Color(32, 51, 43);
    private static final Color SUAVE = new Color(220, 233, 225);
    private static final Color FONDO = new Color(244, 248, 245);
    private static final int MAX_FILAS_POR_DIAPOSITIVA = 12;

    public byte[] generar(ReporteDatos datos) {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(12192000 / 12700, 6858000 / 12700));
            portada(ppt, datos);
            seccionResumen(ppt, datos);
            seccionLecturas(ppt, datos);
            seccionAlertas(ppt, datos);
            seccionConvenio(ppt, datos);
            seccionBitacora(ppt, datos);
            ppt.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PowerPoint", e);
        }
    }

    private void portada(XMLSlideShow ppt, ReporteDatos d) {
        XSLFSlide slide = ppt.createSlide();
        fondo(slide);
        caja(slide, 40, 42, 880, 100, "SOCIEDAD MINERA CERRO VERDE · PTAR LA ENLOZADA", 16, Color.WHITE, VERDE, true);
        caja(slide, 55, 190, 850, 92, d.titulo(), 27, VERDE, FONDO, true);
        caja(slide, 58, 295, 840, 40, "Periodo: " + d.desde() + " al " + d.hasta(), 17, TEXTO, FONDO, true);
        caja(slide, 58, 345, 840, 38, "Estación: " + d.punto(), 15, TEXTO, FONDO, false);
        caja(slide, 58, 395, 840, 38, "Generado por: " + d.generadoPor(), 14, TEXTO, FONDO, false);
        caja(slide, 58, 438, 840, 38, "Generado el: " + d.generadoEn().toString().replace('T', ' '), 13, TEXTO, FONDO, false);
        caja(slide, 58, 486, 840, 42, d.descripcion() == null ? "Reporte del sistema de monitoreo hídrico." : d.descripcion(), 12, TEXTO, SUAVE, false);
    }

    private void seccionResumen(XMLSlideShow ppt, ReporteDatos d) {
        List<String> rows = d.resumen().stream().map(f -> String.format(
                "%s | %s (%s) | Lecturas: %d | Prom.: %s | Mín.: %s | Máx.: %s | Alertas: %d",
                f.punto(), f.parametro(), f.unidad(), f.lecturas(), f.promedio(), f.minimo(), f.maximo(), f.alertas()))
                .toList();
        paginas(ppt, "Resumen estadístico", List.of("Estación | Parámetro | Lecturas | Promedio | Mínimo | Máximo | Alertas"), rows);
    }

    private void seccionLecturas(XMLSlideShow ppt, ReporteDatos d) {
        List<String> rows = d.lecturas().stream().map(f -> String.format("%s | %s | %s | %s %s",
                f.fecha(), f.punto(), f.parametro(), f.valor(), f.unidad())).toList();
        paginas(ppt, "Lecturas de monitoreo", List.of("Fecha y hora | Estación | Parámetro | Valor"), rows);
    }

    private void seccionAlertas(XMLSlideShow ppt, ReporteDatos d) {
        List<String> rows = d.alertas().stream().map(f -> String.format("%s | %s | %s | %s %s | %s | %s | %s",
                f.fecha(), f.punto(), f.parametro(), f.valor(), f.unidad(), f.nivel(), f.tipo(), f.estado())).toList();
        paginas(ppt, "Alertas del periodo", List.of("Fecha | Estación | Parámetro | Valor | Nivel | Tipo | Estado"), rows);
    }

    private void seccionConvenio(XMLSlideShow ppt, ReporteDatos d) {
        CumplimientoResponse c = d.convenio();
        if (c == null) return;
        List<String> rows = new ArrayList<>();
        for (CumplimientoResponse.Mes mes : c.meses()) {
            rows.add(String.format("Mes %02d | Promedio: %s m3/s | Meta: %s m3/s | Cumple: %s | Días con datos: %d",
                    mes.mes(), mes.promedioM3s(), c.metaM3s(), mes.cumpleMeta() == null ? "Sin datos" : (mes.cumpleMeta() ? "Sí" : "No"), mes.diasConDatos()));
        }
        rows.add(String.format("Año %d | Promedio: %s m3/s | Meta: %s m3/s | Cumplimiento: %s | Días con datos: %d",
                c.anio(), c.promedioM3s(), c.metaM3s(), c.cumple() == null ? "Sin datos" : (c.cumple() ? "Sí" : "No"), c.diasConDatos()));
        paginas(ppt, "Cumplimiento del convenio", List.of("Periodo | Indicadores"), rows);
    }

    private void seccionBitacora(XMLSlideShow ppt, ReporteDatos d) {
        if (d.bitacora() == null) return;
        List<String> rows = d.bitacora().stream().map(h -> filaBitacora(h)).toList();
        paginas(ppt, "Bitácora de auditoría", List.of("Fecha y hora | Usuario | Acción | Entidad | Descripción"), rows);
    }

    private String filaBitacora(HistorialResponse h) {
        return String.format("%s | %s | %s | %s #%s | %s", h.fechaHora(), h.usuario(), h.accion(),
                h.entidad(), h.idEntidad(), h.descripcion());
    }

    private void paginas(XMLSlideShow ppt, String titulo, List<String> encabezados, List<String> filas) {
        if (filas.isEmpty()) return;
        int total = (filas.size() + MAX_FILAS_POR_DIAPOSITIVA - 1) / MAX_FILAS_POR_DIAPOSITIVA;
        for (int pagina = 0; pagina < total; pagina++) {
            XSLFSlide slide = ppt.createSlide();
            fondo(slide);
            caja(slide, 28, 20, 900, 52, titulo + " · " + (pagina + 1) + "/" + total, 22, Color.WHITE, VERDE, true);
            caja(slide, 34, 88, 892, 35, encabezados.get(0), 11, VERDE, SUAVE, true);
            int inicio = pagina * MAX_FILAS_POR_DIAPOSITIVA;
            int fin = Math.min(filas.size(), inicio + MAX_FILAS_POR_DIAPOSITIVA);
            int y = 132;
            for (int i = inicio; i < fin; i++) {
                caja(slide, 34, y, 892, 28, filas.get(i), 10, TEXTO, i % 2 == 0 ? FONDO : Color.WHITE, false);
                y += 30;
            }
        }
    }

    private void fondo(XSLFSlide slide) {
        caja(slide, 0, 0, 960, 540, "", 12, TEXTO, FONDO, false);
    }

    private void caja(XSLFSlide slide, int x, int y, int width, int height, String text,
                      int fontSize, Color color, Color fill, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(x, y, width, height));
        box.setFillColor(fill);
        box.setLineColor(fill);
        box.setVerticalAlignment(VerticalAlignment.MIDDLE);
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        paragraph.setSpaceAfter(2.0);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily("Aptos");
        run.setFontSize((double) fontSize);
        run.setFontColor(color);
        run.setBold(bold);
    }
}
