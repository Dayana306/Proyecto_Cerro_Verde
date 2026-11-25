package pe.smcv.ptar.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Serie diaria de un parámetro + límites del umbral vigente (líneas horizontales del gráfico). */
public record TendenciaResponse(
        Integer idParametro,
        String parametro,
        String unidadMedida,
        Integer idPunto,
        String punto,
        LocalDate desde,
        LocalDate hasta,
        Limites umbral,
        List<Dia> dias) {

    public record Dia(LocalDate fecha, BigDecimal promedio, BigDecimal minimo, BigDecimal maximo, int mediciones) {}

    public record Limites(BigDecimal limiteMin, BigDecimal limiteMax, BigDecimal criticoMin, BigDecimal criticoMax) {}
}
