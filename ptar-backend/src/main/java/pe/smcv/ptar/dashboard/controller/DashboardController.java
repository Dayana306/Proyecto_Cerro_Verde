package pe.smcv.ptar.dashboard.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smcv.ptar.dashboard.dto.ResumenResponse;
import pe.smcv.ptar.dashboard.dto.TendenciaResponse;
import pe.smcv.ptar.dashboard.service.DashboardService;

import java.time.LocalDate;

/** Dashboard (RF-04). Disponible para todos los roles. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** Tarjetas de indicadores, conteo de alertas, actividad y convenio. idPunto opcional. */
    @GetMapping("/resumen")
    public ResumenResponse resumen(@RequestParam(required = false) Integer idPunto) {
        return service.resumen(idPunto);
    }

    /** Gráfico de tendencia. Por defecto los últimos 30 días. */
    @GetMapping("/tendencia")
    public TendenciaResponse tendencia(
            @RequestParam Integer idParametro,
            @RequestParam(required = false) Integer idPunto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.tendencia(idParametro, idPunto, desde, hasta);
    }
}
