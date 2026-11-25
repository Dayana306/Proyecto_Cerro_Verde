package pe.smcv.ptar.alerta.dto;

import pe.smcv.ptar.alerta.entity.Alerta;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.alerta.entity.TipoAlerta;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.parametro.entity.Umbral;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AlertaResponse(
        Integer idAlerta,
        NivelAlerta nivel,
        TipoAlerta tipoAlerta,
        EstadoAlerta estado,
        BigDecimal valorDetectado,
        LocalDateTime fechaDeteccion,
        Long idLectura,
        Integer idMonitoreo,
        LocalDateTime horaMuestreo,
        Integer idParametro,
        String parametro,
        String unidadMedida,
        Integer idPunto,
        String punto,
        UmbralAplicado umbral,
        List<SeguimientoResponse> seguimientos) {

    /** Límites con los que se evaluó la lectura. */
    public record UmbralAplicado(Integer idUmbral, BigDecimal limiteMin, BigDecimal limiteMax,
                                 BigDecimal criticoMin, BigDecimal criticoMax) {}

    public static AlertaResponse desde(Alerta a) {
        return desde(a, null);
    }

    public static AlertaResponse desde(Alerta a, List<SeguimientoResponse> seguimientos) {
        LecturaParametro l = a.getLectura();
        Umbral u = a.getUmbral();
        return new AlertaResponse(a.getIdAlerta(), a.getNivel(), a.getTipoAlerta(), a.getEstado(),
                a.getValorDetectado(), a.getFechaDeteccion(),
                l.getIdLectura(), l.getMonitoreo().getIdMonitoreo(), l.getMonitoreo().getHoraMuestreo(),
                l.getParametro().getIdParametro(), l.getParametro().getNombreParametro(),
                l.getParametro().getUnidadMedida(),
                l.getMonitoreo().getPunto().getIdPunto(), l.getMonitoreo().getPunto().getNombrePunto(),
                new UmbralAplicado(u.getIdUmbral(), u.getLimiteMin(), u.getLimiteMax(),
                        u.getCriticoMin(), u.getCriticoMax()),
                seguimientos);
    }
}
