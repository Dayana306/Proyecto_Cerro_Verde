package pe.smcv.ptar.reporte.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.common.util.ExcelUtil;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Reporte en Excel: hojas Resumen, Lecturas, Alertas, Convenio y (si aplica) Bitácora. */
@Component
public class ReporteExcelGenerator {

    public byte[] generar(ReporteDatos d) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet info = wb.createSheet("Información");
            String[][] meta = {
                    {"Reporte", d.titulo()},
                    {"Periodo", d.desde() + " al " + d.hasta()},
                    {"Estación", d.punto()},
                    {"Descripción", d.descripcion() == null ? "" : d.descripcion()},
                    {"Generado por", d.generadoPor()},
                    {"Generado el", d.generadoEn().toString().replace('T', ' ')}};
            for (int i = 0; i < meta.length; i++) {
                Row r = info.createRow(i);
                ExcelUtil.celda(r, 0, meta[i][0]);
                ExcelUtil.celda(r, 1, meta[i][1]);
            }
            info.setColumnWidth(0, 18 * 256);
            info.setColumnWidth(1, 60 * 256);

            Sheet resumen = wb.createSheet("Resumen");
            ExcelUtil.encabezado(wb, resumen, "Estación", "Parámetro", "Unidad", "N° lecturas", "Promedio",
                    "Mínimo", "Máximo", "N° alertas");
            int i = 1;
            for (ReporteDatos.FilaResumen f : d.resumen()) {
                Row r = resumen.createRow(i++);
                ExcelUtil.celda(r, 0, f.punto());
                ExcelUtil.celda(r, 1, f.parametro());
                ExcelUtil.celda(r, 2, f.unidad());
                ExcelUtil.celda(r, 3, f.lecturas());
                ExcelUtil.celda(r, 4, f.promedio());
                ExcelUtil.celda(r, 5, f.minimo());
                ExcelUtil.celda(r, 6, f.maximo());
                ExcelUtil.celda(r, 7, f.alertas());
            }
            ExcelUtil.autoajustar(resumen, 8);

            Sheet lecturas = wb.createSheet("Lecturas");
            ExcelUtil.encabezado(wb, lecturas, "Fecha y hora", "Estación", "Parámetro", "Valor", "Unidad");
            i = 1;
            for (ReporteDatos.FilaLectura f : d.lecturas()) {
                Row r = lecturas.createRow(i++);
                ExcelUtil.celda(r, 0, f.fecha());
                ExcelUtil.celda(r, 1, f.punto());
                ExcelUtil.celda(r, 2, f.parametro());
                ExcelUtil.celda(r, 3, f.valor());
                ExcelUtil.celda(r, 4, f.unidad());
            }
            ExcelUtil.autoajustar(lecturas, 5);

            Sheet alertas = wb.createSheet("Alertas");
            ExcelUtil.encabezado(wb, alertas, "Fecha", "Estación", "Parámetro", "Valor", "Unidad", "Nivel", "Tipo",
                    "Estado");
            i = 1;
            for (ReporteDatos.FilaAlerta f : d.alertas()) {
                Row r = alertas.createRow(i++);
                ExcelUtil.celda(r, 0, f.fecha());
                ExcelUtil.celda(r, 1, f.punto());
                ExcelUtil.celda(r, 2, f.parametro());
                ExcelUtil.celda(r, 3, f.valor());
                ExcelUtil.celda(r, 4, f.unidad());
                ExcelUtil.celda(r, 5, f.nivel());
                ExcelUtil.celda(r, 6, f.tipo());
                ExcelUtil.celda(r, 7, f.estado());
            }
            ExcelUtil.autoajustar(alertas, 8);

            CumplimientoResponse c = d.convenio();
            if (c != null) {
                Sheet conv = wb.createSheet("Convenio");
                ExcelUtil.encabezado(wb, conv, "Mes", "Promedio (m3/s)", "Meta (m3/s)", "¿Cumple?", "Días con datos");
                i = 1;
                for (CumplimientoResponse.Mes m : c.meses()) {
                    Row r = conv.createRow(i++);
                    ExcelUtil.celda(r, 0, c.anio() + "-" + String.format("%02d", m.mes()));
                    ExcelUtil.celda(r, 1, m.promedioM3s());
                    ExcelUtil.celda(r, 2, c.metaM3s());
                    ExcelUtil.celda(r, 3, m.cumpleMeta() == null ? "-" : (m.cumpleMeta() ? "Sí" : "No"));
                    ExcelUtil.celda(r, 4, m.diasConDatos());
                }
                Row total = conv.createRow(i);
                ExcelUtil.celda(total, 0, "Año " + c.anio());
                ExcelUtil.celda(total, 1, c.promedioM3s());
                ExcelUtil.celda(total, 2, c.metaM3s());
                ExcelUtil.celda(total, 3, c.cumple() == null ? "-" : (c.cumple() ? "Sí" : "No"));
                ExcelUtil.celda(total, 4, c.diasConDatos());
                ExcelUtil.autoajustar(conv, 5);
            }

            if (d.bitacora() != null) {
                Sheet bit = wb.createSheet("Bitácora");
                ExcelUtil.encabezado(wb, bit, "Fecha y hora", "Usuario", "Acción", "Entidad", "ID", "Descripción", "IP");
                i = 1;
                for (HistorialResponse h : d.bitacora()) {
                    Row r = bit.createRow(i++);
                    ExcelUtil.celda(r, 0, h.fechaHora());
                    ExcelUtil.celda(r, 1, h.usuario());
                    ExcelUtil.celda(r, 2, h.accion().name());
                    ExcelUtil.celda(r, 3, h.entidad());
                    ExcelUtil.celda(r, 4, h.idEntidad());
                    ExcelUtil.celda(r, 5, h.descripcion());
                    ExcelUtil.celda(r, 6, h.ipOrigen());
                }
                ExcelUtil.autoajustar(bit, 7);
            }
            return ExcelUtil.aBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }
}
