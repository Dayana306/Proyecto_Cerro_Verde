package pe.smcv.ptar.convenio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cumplimiento del convenio en un año (CU10).
 * cumple = null cuando no hay mediciones. porcentajeMeta = promedio / meta * 100.
 */
public record CumplimientoResponse(
        Integer idConvenio,
        String convenio,
        int anio,
        LocalDate desde,
        LocalDate hasta,
        String punto,
        BigDecimal metaM3s,
        BigDecimal promedioM3s,
        BigDecimal porcentajeMeta,
        Boolean cumple,
        int diasConDatos,
        int mediciones,
        String metodo,
        List<Mes> meses) {

    public record Mes(int mes, BigDecimal promedioM3s, Boolean cumpleMeta, int diasConDatos, int mediciones) {}
}
