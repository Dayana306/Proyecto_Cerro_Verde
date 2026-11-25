package pe.smcv.ptar.auditoria.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.common.util.ExcelUtil;

import java.time.LocalDate;
import java.util.List;

@Service
public class AuditoriaExportService {

    private final AuditoriaService auditoriaService;

    public AuditoriaExportService(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public byte[] exportarExcel(String entidad, AccionAuditoria accion, Integer idUsuario,
                                LocalDate desde, LocalDate hasta) {
        List<HistorialResponse> filas = auditoriaService.listar(entidad, accion, idUsuario, desde, hasta);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Bitácora");
            ExcelUtil.encabezado(wb, hoja, "Fecha y hora", "Usuario", "Acción", "Entidad", "ID",
                    "Descripción", "IP", "Datos anteriores", "Datos nuevos");
            int r = 1;
            for (HistorialResponse h : filas) {
                Row row = hoja.createRow(r++);
                ExcelUtil.celda(row, 0, h.fechaHora());
                ExcelUtil.celda(row, 1, h.usuario());
                ExcelUtil.celda(row, 2, h.accion().name());
                ExcelUtil.celda(row, 3, h.entidad());
                ExcelUtil.celda(row, 4, h.idEntidad());
                ExcelUtil.celda(row, 5, h.descripcion());
                ExcelUtil.celda(row, 6, h.ipOrigen());
                ExcelUtil.celda(row, 7, h.datosAnteriores());
                ExcelUtil.celda(row, 8, h.datosNuevos());
            }
            ExcelUtil.autoajustar(hoja, 7);
            byte[] bytes = ExcelUtil.aBytes(wb);
            auditoriaService.registrar(AccionAuditoria.EXPORTAR, "HISTORIAL", null,
                    "Exportación de bitácora (" + filas.size() + " registros)", null, null);
            return bytes;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel de auditoría", e);
        }
    }
}
