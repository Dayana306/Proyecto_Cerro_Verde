package pe.smcv.ptar.monitoreo.dto;

import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.monitoreo.entity.Monitoreo;
import pe.smcv.ptar.monitoreo.entity.TipoMuestreo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * lecturas: solo las ACTIVAS. alertas: las alertas del monitoreo (solo en detalle y al registrar/editar).
 */
public record MonitoreoResponse(
        Integer idMonitoreo,
        TipoMuestreo tipoMuestreo,
        LocalDateTime horaMuestreo,
        LocalDateTime fechaRegistro,
        String observacion,
        Estado estado,
        Integer idPunto,
        String punto,
        Integer idUsuario,
        String registradoPor,
        List<LecturaResponse> lecturas,
        List<AlertaResponse> alertas) {

    /** nivelAlerta: nivel de la alerta sin cerrar de esa lectura (null si no tiene). Solo se llena en el detalle. */
    public record LecturaResponse(Long idLectura, Integer idParametro, String codigo, String parametro,
                                  String unidadMedida, BigDecimal valor, NivelAlerta nivelAlerta) {}

    public static MonitoreoResponse desde(Monitoreo m) {
        return desde(m, Map.of(), null);
    }

    public static MonitoreoResponse desde(Monitoreo m, Map<Long, NivelAlerta> nivelPorLectura,
                                          List<AlertaResponse> alertas) {
        List<LecturaResponse> lecturas = m.getLecturas().stream()
                .filter(l -> l.getEstado() == Estado.ACTIVO)
                .map(l -> lectura(l, nivelPorLectura.get(l.getIdLectura())))
                .toList();
        return new MonitoreoResponse(m.getIdMonitoreo(), m.getTipoMuestreo(), m.getHoraMuestreo(),
                m.getFechaRegistro(), m.getObservacion(), m.getEstado(),
                m.getPunto().getIdPunto(), m.getPunto().getNombrePunto(),
                m.getUsuario().getIdUsuario(), m.getUsuario().getNombreCompleto(),
                lecturas, alertas);
    }

    private static LecturaResponse lectura(LecturaParametro l, NivelAlerta nivel) {
        return new LecturaResponse(l.getIdLectura(), l.getParametro().getIdParametro(), l.getParametro().getCodigo(),
                l.getParametro().getNombreParametro(), l.getParametro().getUnidadMedida(), l.getValor(), nivel);
    }
}
