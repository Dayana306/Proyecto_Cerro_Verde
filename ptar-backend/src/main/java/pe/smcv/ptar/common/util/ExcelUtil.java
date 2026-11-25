package pe.smcv.ptar.common.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Utilidades mínimas de Apache POI para los Excel del sistema. */
public final class ExcelUtil {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExcelUtil() {
    }

    public static void encabezado(Workbook wb, Sheet hoja, String... titulos) {
        CellStyle estilo = wb.createCellStyle();
        Font fuente = wb.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Row row = hoja.createRow(0);
        for (int i = 0; i < titulos.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(titulos[i]);
            c.setCellStyle(estilo);
        }
        hoja.createFreezePane(0, 1);
    }

    public static void celda(Row row, int col, Object valor) {
        Cell c = row.createCell(col);
        if (valor == null) {
            c.setBlank();
        } else if (valor instanceof BigDecimal bd) {
            c.setCellValue(bd.doubleValue());
        } else if (valor instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else if (valor instanceof LocalDateTime ldt) {
            c.setCellValue(ldt.format(FECHA_HORA));
        } else if (valor instanceof LocalDate ld) {
            c.setCellValue(ld.format(FECHA));
        } else {
            String texto = valor.toString();
            // Límite de Excel por celda: 32 767 caracteres.
            c.setCellValue(texto.length() > 32000 ? texto.substring(0, 32000) : texto);
        }
    }

    /**
     * Ancho fijo de ~22 caracteres para las primeras n columnas.
     * (No se usa autoSizeColumn porque necesita fuentes del sistema y falla en contenedores sin ellas.)
     */
    public static void autoajustar(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.setColumnWidth(i, 22 * 256);
        }
    }

    public static byte[] aBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
