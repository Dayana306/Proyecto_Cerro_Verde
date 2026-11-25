package pe.smcv.ptar.reporte.dto;

import pe.smcv.ptar.reporte.entity.EstadoReporte;
import pe.smcv.ptar.reporte.entity.FormatoReporte;
import pe.smcv.ptar.reporte.entity.Reporte;
import pe.smcv.ptar.reporte.entity.ReporteDetalle;
import pe.smcv.ptar.reporte.entity.TipoReporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** detalles y envios solo se incluyen en el detalle (GET /api/reportes/{id}) y al generar. */
public record ReporteResponse(
        Integer idReporte,
        TipoReporte tipoReporte,
        FormatoReporte formato,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Integer idPunto,
        String punto,
        String descripcion,
        String nombreArchivo,
        EstadoReporte estado,
        LocalDateTime fechaGeneracion,
        String generadoPor,
        String urlDescarga,
        List<Detalle> detalles,
        List<EnvioResponse> envios) {

    public record Detalle(Integer idPunto, String punto, Integer idParametro, String parametro, String unidadMedida,
                          int cantidadLecturas, BigDecimal promedio, BigDecimal minimo, BigDecimal maximo,
                          int cantidadAlertas) {
        public static Detalle desde(ReporteDetalle d) {
            return new Detalle(d.getPunto().getIdPunto(), d.getPunto().getNombrePunto(),
                    d.getParametro().getIdParametro(), d.getParametro().getNombreParametro(),
                    d.getParametro().getUnidadMedida(), d.getCantidadLecturas(), d.getValorPromedio(),
                    d.getValorMinimo(), d.getValorMaximo(), d.getCantidadAlertas());
        }
    }

    public static ReporteResponse desde(Reporte r) {
        return desde(r, null, null);
    }

    public static ReporteResponse desde(Reporte r, List<Detalle> detalles, List<EnvioResponse> envios) {
        return new ReporteResponse(r.getIdReporte(), r.getTipoReporte(), r.getFormato(), r.getFechaDesde(),
                r.getFechaHasta(),
                r.getPunto() == null ? null : r.getPunto().getIdPunto(),
                r.getPunto() == null ? "Todas las estaciones" : r.getPunto().getNombrePunto(),
                r.getDescripcion(), r.getNombreArchivo(), r.getEstado(), r.getFechaGeneracion(),
                r.getUsuario().getNombreCompleto(), "/api/reportes/" + r.getIdReporte() + "/descarga",
                detalles, envios);
    }
}
