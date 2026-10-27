package pe.smcv.ptar.reporte.service;

import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;
import pe.smcv.ptar.reporte.entity.TipoReporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Todo el contenido de un reporte, independiente del formato (PDF o Excel). */
public record ReporteDatos(
        String titulo,
        TipoReporte tipo,
        LocalDate desde,
        LocalDate hasta,
        String punto,
        String descripcion,
        String generadoPor,
        LocalDateTime generadoEn,
        List<FilaResumen> resumen,
        List<FilaLectura> lecturas,
        List<FilaAlerta> alertas,
        List<HistorialResponse> bitacora,
        CumplimientoResponse convenio) {

    public record FilaResumen(Integer idPunto, String punto, Integer idParametro, String parametro, String unidad,
                              int lecturas, BigDecimal promedio, BigDecimal minimo, BigDecimal maximo, int alertas) {}

    public record FilaLectura(LocalDateTime fecha, String punto, String parametro, String unidad, BigDecimal valor) {}

    public record FilaAlerta(LocalDateTime fecha, String punto, String parametro, BigDecimal valor, String unidad,
                             String nivel, String tipo, String estado) {}
}
