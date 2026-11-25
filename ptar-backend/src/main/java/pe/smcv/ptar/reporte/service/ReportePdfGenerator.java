package pe.smcv.ptar.reporte.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reporte en PDF (OpenPDF). Incluye resumen estadístico, alertas, convenio y, en reportes de
 * AUDITORIA, la bitácora. El detalle lectura por lectura solo va en el formato Excel.
 */
@Component
public class ReportePdfGenerator {

    private static final int MAX_FILAS_ALERTAS = 300;
    private static final int MAX_FILAS_BITACORA = 500;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color GRIS = new Color(230, 233, 237);
    private static final Color AZUL = new Color(20, 70, 110);

    private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL);
    private final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, AZUL);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

    public byte[] generar(ReporteDatos d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph(d.titulo(), titulo));
            doc.add(new Paragraph("Sistema de monitoreo de indicadores hídricos - PTAR La Enlozada", pequena));
            doc.add(Chunk.NEWLINE);
            PdfPTable meta = tabla(new float[]{1.2f, 5f});
            meta.setHeaderRows(0);
            fila(meta, "Periodo", d.desde() + " al " + d.hasta());
            fila(meta, "Estación", d.punto());
            if (d.descripcion() != null) {
                fila(meta, "Descripción", d.descripcion());
            }
            fila(meta, "Generado por", d.generadoPor() + " el " + d.generadoEn().format(FMT));
            doc.add(meta);

            seccion(doc, "1. Resumen estadístico por estación y parámetro");
            if (d.resumen().isEmpty()) {
                doc.add(new Paragraph("No hay lecturas registradas en el periodo.", normal));
            } else {
                PdfPTable t = tabla(new float[]{3, 2, 1, 1, 1.2f, 1.2f, 1.2f, 1});
                encabezado(t, "Estación", "Parámetro", "Unidad", "N°", "Promedio", "Mínimo", "Máximo", "Alertas");
                for (ReporteDatos.FilaResumen f : d.resumen()) {
                    celdas(t, f.punto(), f.parametro(), f.unidad(), String.valueOf(f.lecturas()), num(f.promedio()),
                            num(f.minimo()), num(f.maximo()), String.valueOf(f.alertas()));
                }
                doc.add(t);
            }

            seccion(doc, "2. Alertas del periodo (" + d.alertas().size() + ")");
            if (d.alertas().isEmpty()) {
                doc.add(new Paragraph("No se registraron alertas en el periodo.", normal));
            } else {
                PdfPTable t = tabla(new float[]{1.6f, 3, 2, 1.2f, 1, 1, 1.8f, 1.3f});
                encabezado(t, "Fecha", "Estación", "Parámetro", "Valor", "Unidad", "Nivel", "Tipo", "Estado");
                d.alertas().stream().limit(MAX_FILAS_ALERTAS).forEach(a -> celdas(t, fecha(a.fecha()), a.punto(),
                        a.parametro(), num(a.valor()), a.unidad(), a.nivel(), a.tipo(), a.estado()));
                doc.add(t);
                if (d.alertas().size() > MAX_FILAS_ALERTAS) {
                    doc.add(new Paragraph("Se muestran las primeras " + MAX_FILAS_ALERTAS
                            + " alertas. El listado completo está en el formato Excel.", pequena));
                }
            }

            CumplimientoResponse c = d.convenio();
            if (c != null) {
                seccion(doc, "3. Convenio hídrico: " + c.convenio() + " (año " + c.anio() + ")");
                String estado = c.cumple() == null ? "Sin datos suficientes"
                        : (c.cumple() ? "CUMPLE" : "NO CUMPLE");
                doc.add(new Paragraph(String.format("Meta: %s m3/s  |  Promedio: %s m3/s  |  %s  |  %s",
                        num(c.metaM3s()), num(c.promedioM3s()),
                        c.porcentajeMeta() == null ? "-" : c.porcentajeMeta() + " % de la meta", estado), negrita));
                doc.add(new Paragraph("Punto de medición: " + c.punto() + ". " + c.metodo() + ".", pequena));
            }

            if (d.bitacora() != null) {
                seccion(doc, "4. Bitácora de auditoría (" + d.bitacora().size() + " registros)");
                PdfPTable t = tabla(new float[]{1.6f, 2.2f, 1.6f, 1.6f, 5});
                encabezado(t, "Fecha", "Usuario", "Acción", "Entidad", "Descripción");
                for (HistorialResponse h : d.bitacora().stream().limit(MAX_FILAS_BITACORA).toList()) {
                    celdas(t, fecha(h.fechaHora()), h.usuario(), h.accion().name(), h.entidad(),
                            h.descripcion() == null ? "" : h.descripcion());
                }
                doc.add(t);
                if (d.bitacora().size() > MAX_FILAS_BITACORA) {
                    doc.add(new Paragraph("Se muestran los primeros " + MAX_FILAS_BITACORA
                            + " registros. La bitácora completa está en el formato Excel.", pequena));
                }
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void seccion(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, subtitulo);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPTable tabla(float[] anchos) {
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        return t;
    }

    private void encabezado(PdfPTable t, String... titulos) {
        for (String s : titulos) {
            PdfPCell c = new PdfPCell(new Phrase(s, negrita));
            c.setBackgroundColor(GRIS);
            c.setPadding(4);
            t.addCell(c);
        }
    }

    private void celdas(PdfPTable t, String... valores) {
        for (String v : valores) {
            PdfPCell c = new PdfPCell(new Phrase(v == null ? "" : v, normal));
            c.setPadding(3);
            t.addCell(c);
        }
    }

    private void fila(PdfPTable t, String etiqueta, String valor) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, negrita));
        a.setBorder(Rectangle.NO_BORDER);
        PdfPCell b = new PdfPCell(new Phrase(valor, normal));
        b.setBorder(Rectangle.NO_BORDER);
        t.addCell(a);
        t.addCell(b);
    }

    private static String num(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String fecha(LocalDateTime f) {
        return f == null ? "" : f.format(FMT);
    }
}
