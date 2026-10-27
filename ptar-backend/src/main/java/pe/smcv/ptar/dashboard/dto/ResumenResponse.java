package pe.smcv.ptar.dashboard.dto;

import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos de la pantalla principal (RF-04). */
public record ResumenResponse(
        LocalDateTime generadoEn,
        Integer idPunto,
        List<Indicador> indicadores,
        ConteoAlertas alertas,
        long monitoreosHoy,
        long monitoreosUltimos30Dias,
        long puntosActivos,
        CumplimientoResponse convenio,
        List<AlertaResponse> alertasRecientes) {

    /**
     * Última lectura de cada parámetro. estado: NORMAL | MEDIA | ALTA | SIN_UMBRAL | SIN_DATOS
     * (sirve para el color de la tarjeta).
     */
    public record Indicador(Integer idParametro, String codigo, String parametro, String unidadMedida,
                            BigDecimal ultimoValor, LocalDateTime fecha, Integer idPunto, String punto,
                            String estado, BigDecimal limiteMin, BigDecimal limiteMax) {}

    public record ConteoAlertas(long noCerradas, long alta, long media) {}
}
