package pe.smcv.ptar.dashboard.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.convenio.service.ConvenioService;
import pe.smcv.ptar.dashboard.dto.ResumenResponse;
import pe.smcv.ptar.dashboard.dto.TendenciaResponse;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.monitoreo.repository.LecturaRepository;
import pe.smcv.ptar.monitoreo.repository.MonitoreoRepository;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.entity.Umbral;
import pe.smcv.ptar.parametro.repository.ParametroRepository;
import pe.smcv.ptar.parametro.service.EvaluadorUmbral;
import pe.smcv.ptar.parametro.service.ParametroService;
import pe.smcv.ptar.parametro.service.UmbralService;
import pe.smcv.ptar.ubicacion.repository.PuntoMonitoreoRepository;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Indicadores del dashboard (RF-04). Solo lectura; consultas acotadas para responder en < 3 s (RNF-01). */
@Service
public class DashboardService {

    private static final int MAX_DIAS_TENDENCIA = 366;

    private final ParametroRepository parametroRepository;
    private final ParametroService parametroService;
    private final LecturaRepository lecturaRepository;
    private final MonitoreoRepository monitoreoRepository;
    private final AlertaRepository alertaRepository;
    private final PuntoMonitoreoRepository puntoRepository;
    private final PuntoMonitoreoService puntoService;
    private final UmbralService umbralService;
    private final ConvenioService convenioService;

    public DashboardService(ParametroRepository parametroRepository, ParametroService parametroService,
                            LecturaRepository lecturaRepository, MonitoreoRepository monitoreoRepository,
                            AlertaRepository alertaRepository, PuntoMonitoreoRepository puntoRepository,
                            PuntoMonitoreoService puntoService, UmbralService umbralService,
                            ConvenioService convenioService) {
        this.parametroRepository = parametroRepository;
        this.parametroService = parametroService;
        this.lecturaRepository = lecturaRepository;
        this.monitoreoRepository = monitoreoRepository;
        this.alertaRepository = alertaRepository;
        this.puntoRepository = puntoRepository;
        this.puntoService = puntoService;
        this.umbralService = umbralService;
        this.convenioService = convenioService;
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(Integer idPunto) {
        if (idPunto != null) {
            puntoService.buscar(idPunto);
        }
        List<ResumenResponse.Indicador> indicadores = new ArrayList<>();
        for (Parametro p : parametroRepository.findByEstado(Estado.ACTIVO, Sort.by("nombreParametro"))) {
            indicadores.add(indicador(p, idPunto));
        }

        long alta = 0;
        long media = 0;
        for (Object[] fila : alertaRepository.contarNoCerradasPorNivel(idPunto)) {
            long n = ((Number) fila[1]).longValue();
            if ("ALTA".equals(String.valueOf(fila[0]))) {
                alta += n;
            } else {
                media += n;
            }
        }

        LocalDate hoy = LocalDate.now();
        long monitoreosHoy = monitoreoRepository.contarActivos(idPunto, Fechas.inicio(hoy), Fechas.fin(hoy));
        long monitoreos30 = monitoreoRepository.contarActivos(idPunto, Fechas.inicio(hoy.minusDays(29)), Fechas.fin(hoy));

        List<AlertaResponse> recientes = alertaRepository.recientesNoCerradas(idPunto, PageRequest.of(0, 5)).stream()
                .map(AlertaResponse::desde).toList();

        return new ResumenResponse(LocalDateTime.now(), idPunto, indicadores,
                new ResumenResponse.ConteoAlertas(alta + media, alta, media),
                monitoreosHoy, monitoreos30, puntoRepository.countByEstado(Estado.ACTIVO),
                convenioService.cumplimientoVigente().orElse(null), recientes);
    }

    /** Serie diaria (promedio, mínimo, máximo) de un parámetro en los últimos N días. */
    @Transactional(readOnly = true)
    public TendenciaResponse tendencia(Integer idParametro, Integer idPunto, LocalDate desde, LocalDate hasta) {
        Parametro p = parametroService.buscar(idParametro);
        String nombrePunto = idPunto == null ? "Todos los puntos" : puntoService.buscar(idPunto).getNombrePunto();
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(29) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("'desde' no puede ser posterior a 'hasta'");
        }
        if (inicio.plusDays(MAX_DIAS_TENDENCIA).isBefore(fin)) {
            throw new ReglaNegocioException("El rango máximo es de " + MAX_DIAS_TENDENCIA + " días");
        }

        SortedMap<LocalDate, List<BigDecimal>> porDia = new TreeMap<>();
        for (LecturaParametro l : lecturaRepository.serie(idParametro, idPunto, Fechas.inicio(inicio), Fechas.fin(fin))) {
            porDia.computeIfAbsent(l.getMonitoreo().getHoraMuestreo().toLocalDate(), d -> new ArrayList<>())
                    .add(l.getValor());
        }
        List<TendenciaResponse.Dia> dias = new ArrayList<>();
        porDia.forEach((dia, valores) -> {
            BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            dias.add(new TendenciaResponse.Dia(dia,
                    suma.divide(BigDecimal.valueOf(valores.size()), 3, RoundingMode.HALF_UP),
                    Collections.min(valores), Collections.max(valores), valores.size()));
        });

        TendenciaResponse.Limites limites = umbralService.vigente(idParametro, idPunto)
                .map(u -> new TendenciaResponse.Limites(u.getLimiteMin(), u.getLimiteMax(),
                        u.getCriticoMin(), u.getCriticoMax()))
                .orElse(null);

        return new TendenciaResponse(p.getIdParametro(), p.getNombreParametro(), p.getUnidadMedida(), idPunto,
                nombrePunto, inicio, fin, limites, dias);
    }

    private ResumenResponse.Indicador indicador(Parametro p, Integer idPunto) {
        List<LecturaParametro> ultimas = lecturaRepository.ultimas(p.getIdParametro(), idPunto, PageRequest.of(0, 1));
        if (ultimas.isEmpty()) {
            return new ResumenResponse.Indicador(p.getIdParametro(), p.getCodigo(), p.getNombreParametro(),
                    p.getUnidadMedida(), null, null, null, null, "SIN_DATOS", null, null);
        }
        LecturaParametro l = ultimas.get(0);
        Integer puntoLectura = l.getMonitoreo().getPunto().getIdPunto();
        Optional<Umbral> umbral = umbralService.vigente(p.getIdParametro(), puntoLectura);
        String estado = umbral
                .map(u -> EvaluadorUmbral.evaluar(l.getValor(), u.getLimiteMin(), u.getLimiteMax(),
                                u.getCriticoMin(), u.getCriticoMax())
                        .map(r -> r.nivel().name())
                        .orElse("NORMAL"))
                .orElse("SIN_UMBRAL");
        return new ResumenResponse.Indicador(p.getIdParametro(), p.getCodigo(), p.getNombreParametro(),
                p.getUnidadMedida(), l.getValor(), l.getMonitoreo().getHoraMuestreo(), puntoLectura,
                l.getMonitoreo().getPunto().getNombrePunto(), estado,
                umbral.map(Umbral::getLimiteMin).orElse(null), umbral.map(Umbral::getLimiteMax).orElse(null));
    }
}
