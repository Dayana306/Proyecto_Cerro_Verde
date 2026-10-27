package pe.smcv.ptar.reporte.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.entity.Alerta;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.AlmacenamientoService;
import pe.smcv.ptar.common.util.Descargas;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;
import pe.smcv.ptar.convenio.service.ConvenioService;
import pe.smcv.ptar.convenio.service.EntidadExternaService;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.monitoreo.repository.LecturaRepository;
import pe.smcv.ptar.parametro.service.ParametroService;
import pe.smcv.ptar.reporte.dto.EnvioRequest;
import pe.smcv.ptar.reporte.dto.EnvioResponse;
import pe.smcv.ptar.reporte.dto.ReporteRequest;
import pe.smcv.ptar.reporte.dto.ReporteResponse;
import pe.smcv.ptar.reporte.entity.*;
import pe.smcv.ptar.reporte.repository.ReporteDetalleRepository;
import pe.smcv.ptar.reporte.repository.ReporteEnvioRepository;
import pe.smcv.ptar.reporte.repository.ReporteRepository;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Reportes ambientales y de auditoría (RF-07, CU06). */
@Service
public class ReporteService {

    private static final String ENTIDAD = "REPORTE";
    private static final long MAX_DIAS = 366;

    private final ReporteRepository repo;
    private final ReporteDetalleRepository detalleRepository;
    private final ReporteEnvioRepository envioRepository;
    private final LecturaRepository lecturaRepository;
    private final AlertaRepository alertaRepository;
    private final PuntoMonitoreoService puntoService;
    private final ParametroService parametroService;
    private final EntidadExternaService entidadService;
    private final ConvenioService convenioService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;
    private final AlmacenamientoService almacenamiento;
    private final ReportePdfGenerator pdf;
    private final ReporteExcelGenerator excel;

    public ReporteService(ReporteRepository repo, ReporteDetalleRepository detalleRepository,
                          ReporteEnvioRepository envioRepository, LecturaRepository lecturaRepository,
                          AlertaRepository alertaRepository, PuntoMonitoreoService puntoService,
                          ParametroService parametroService, EntidadExternaService entidadService,
                          ConvenioService convenioService, UsuarioRepository usuarioRepository,
                          AuditoriaService auditoria, AlmacenamientoService almacenamiento,
                          ReportePdfGenerator pdf, ReporteExcelGenerator excel) {
        this.repo = repo;
        this.detalleRepository = detalleRepository;
        this.envioRepository = envioRepository;
        this.lecturaRepository = lecturaRepository;
        this.alertaRepository = alertaRepository;
        this.puntoService = puntoService;
        this.parametroService = parametroService;
        this.entidadService = entidadService;
        this.convenioService = convenioService;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
        this.almacenamiento = almacenamiento;
        this.pdf = pdf;
        this.excel = excel;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReporteResponse> buscar(TipoReporte tipo, EstadoReporte estado, Pageable pageable) {
        return PageResponse.from(repo.buscar(tipo, estado, pageable), ReporteResponse::desde);
    }

    @Transactional(readOnly = true)
    public ReporteResponse obtener(Integer id) {
        return completo(buscar(id));
    }

    /**
     * Genera el archivo, lo guarda y registra el reporte con su detalle estadístico.
     * El archivo queda disponible en GET /api/reportes/{id}/descarga.
     */
    @Transactional
    public ReporteResponse generar(ReporteRequest req, Integer idUsuario) {
        if (req.fechaDesde().isAfter(req.fechaHasta())) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(req.fechaDesde(), req.fechaHasta()) >= MAX_DIAS) {
            throw new ReglaNegocioException("El periodo máximo de un reporte es de " + MAX_DIAS + " días");
        }
        PuntoMonitoreo punto = req.idPunto() == null ? null : puntoService.buscar(req.idPunto());
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + idUsuario));
        Integer idPunto = punto == null ? null : punto.getIdPunto();
        LocalDateTime desde = Fechas.inicio(req.fechaDesde());
        LocalDateTime hasta = Fechas.fin(req.fechaHasta());

        List<LecturaParametro> lecturas = lecturaRepository.periodo(idPunto, desde, hasta);
        List<Alerta> alertas = alertaRepository.periodo(idPunto, desde, hasta);
        List<ReporteDatos.FilaResumen> resumen = resumir(lecturas, alertas);

        List<HistorialResponse> bitacora = req.tipoReporte() == TipoReporte.AUDITORIA
                ? auditoria.listar(null, null, null, req.fechaDesde(), req.fechaHasta())
                : null;
        CumplimientoResponse convenio = convenioService.cumplimientoVigente()
                .map(c -> convenioService.cumplimiento(c.idConvenio(), req.fechaHasta().getYear()))
                .orElse(null);

        LocalDateTime ahora = LocalDateTime.now();
        ReporteDatos datos = new ReporteDatos(
                titulo(req.tipoReporte()), req.tipoReporte(), req.fechaDesde(), req.fechaHasta(),
                punto == null ? "Todas las estaciones" : punto.getNombrePunto(),
                limpiar(req.descripcion()), usuario.getNombreCompleto(), ahora, resumen,
                lecturas.stream().map(l -> new ReporteDatos.FilaLectura(l.getMonitoreo().getHoraMuestreo(),
                        l.getMonitoreo().getPunto().getNombrePunto(), l.getParametro().getNombreParametro(),
                        l.getParametro().getUnidadMedida(), l.getValor())).toList(),
                alertas.stream().map(a -> new ReporteDatos.FilaAlerta(a.getFechaDeteccion(),
                        a.getLectura().getMonitoreo().getPunto().getNombrePunto(),
                        a.getLectura().getParametro().getNombreParametro(), a.getValorDetectado(),
                        a.getLectura().getParametro().getUnidadMedida(), a.getNivel().name(),
                        a.getTipoAlerta().name(), a.getEstado().name())).toList(),
                bitacora, convenio);

        boolean esPdf = req.formato() == FormatoReporte.PDF;
        byte[] contenido = esPdf ? pdf.generar(datos) : excel.generar(datos);
        String nombre = String.format("reporte_%s_%s_%s.%s", req.tipoReporte().name().toLowerCase(Locale.ROOT),
                req.fechaDesde().format(DateTimeFormatter.BASIC_ISO_DATE),
                req.fechaHasta().format(DateTimeFormatter.BASIC_ISO_DATE), esPdf ? "pdf" : "xlsx");

        Reporte r = new Reporte();
        r.setTipoReporte(req.tipoReporte());
        r.setFormato(req.formato());
        r.setFechaDesde(req.fechaDesde());
        r.setFechaHasta(req.fechaHasta());
        r.setPunto(punto);
        r.setDescripcion(limpiar(req.descripcion()));
        r.setUsuario(usuario);
        r.setNombreArchivo(nombre);
        r.setRutaArchivo(almacenamiento.guardar("reportes", nombre, contenido));
        repo.save(r);

        for (ReporteDatos.FilaResumen f : resumen) {
            ReporteDetalle det = new ReporteDetalle();
            det.setReporte(r);
            det.setPunto(puntoService.buscar(f.idPunto()));
            det.setParametro(parametroService.buscar(f.idParametro()));
            det.setCantidadLecturas(f.lecturas());
            det.setValorPromedio(f.promedio());
            det.setValorMinimo(f.minimo());
            det.setValorMaximo(f.maximo());
            det.setCantidadAlertas(f.alertas());
            detalleRepository.save(det);
        }

        auditoria.registrar(AccionAuditoria.GENERAR, ENTIDAD, r.getIdReporte(),
                "Reporte " + req.tipoReporte() + " " + req.formato() + " del " + req.fechaDesde() + " al "
                        + req.fechaHasta() + " (" + lecturas.size() + " lecturas, " + alertas.size() + " alertas)",
                null, null);
        return completo(r);
    }

    @Transactional(readOnly = true)
    public Archivo descargar(Integer id) {
        Reporte r = buscar(id);
        String tipo = r.getFormato() == FormatoReporte.PDF ? Descargas.PDF : Descargas.XLSX;
        return new Archivo(r.getNombreArchivo(), tipo, almacenamiento.leer(r.getRutaArchivo()));
    }

    /** El reporte anulado se conserva (trazabilidad), pero deja de considerarse vigente. */
    @Transactional
    public ReporteResponse anular(Integer id) {
        Reporte r = buscar(id);
        if (r.getEstado() == EstadoReporte.ANULADO) {
            throw new ReglaNegocioException("El reporte ya está anulado");
        }
        r.setEstado(EstadoReporte.ANULADO);
        repo.save(r);
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id, "Reporte #" + id + " anulado", null, null);
        return completo(r);
    }

    /** Deja constancia de que el reporte se entregó a una entidad (ANA, OEFA, SEDAPAR...). */
    @Transactional
    public EnvioResponse registrarEnvio(Integer id, EnvioRequest req, Integer idUsuario) {
        Reporte r = buscar(id);
        if (r.getEstado() == EstadoReporte.ANULADO) {
            throw new ReglaNegocioException("No se puede registrar el envío de un reporte anulado");
        }
        ReporteEnvio e = new ReporteEnvio();
        e.setReporte(r);
        e.setEntidad(entidadService.buscarActiva(req.idEntidad()));
        e.setMedio(req.medio());
        e.setFechaEnvio(req.fechaEnvio() == null ? LocalDateTime.now() : req.fechaEnvio());
        e.setObservacion(limpiar(req.observacion()));
        e.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        EnvioResponse res = EnvioResponse.desde(envioRepository.save(e));
        auditoria.registrar(AccionAuditoria.CREAR, "REPORTE_ENVIO", res.idEnvio(),
                "Reporte #" + id + " entregado a " + res.entidad() + " por " + res.medio(), null, res);
        return res;
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> envios(Integer id) {
        buscar(id);
        return envioRepository.deReporte(id).stream().map(EnvioResponse::desde).toList();
    }

    // ------------------------------------------------------------------

    /** Agrupa por (punto, parámetro): cantidad, promedio, mínimo, máximo y alertas. */
    static List<ReporteDatos.FilaResumen> resumir(List<LecturaParametro> lecturas, List<Alerta> alertas) {
        Map<String, Integer> alertasPorClave = new HashMap<>();
        for (Alerta a : alertas) {
            alertasPorClave.merge(clave(a.getLectura()), 1, Integer::sum);
        }
        Map<String, List<LecturaParametro>> grupos = new TreeMap<>();
        for (LecturaParametro l : lecturas) {
            grupos.computeIfAbsent(clave(l), k -> new ArrayList<>()).add(l);
        }
        List<ReporteDatos.FilaResumen> filas = new ArrayList<>();
        for (Map.Entry<String, List<LecturaParametro>> g : grupos.entrySet()) {
            List<LecturaParametro> ls = g.getValue();
            LecturaParametro primera = ls.get(0);
            BigDecimal suma = BigDecimal.ZERO;
            BigDecimal min = null;
            BigDecimal max = null;
            for (LecturaParametro l : ls) {
                suma = suma.add(l.getValor());
                min = min == null || l.getValor().compareTo(min) < 0 ? l.getValor() : min;
                max = max == null || l.getValor().compareTo(max) > 0 ? l.getValor() : max;
            }
            filas.add(new ReporteDatos.FilaResumen(
                    primera.getMonitoreo().getPunto().getIdPunto(), primera.getMonitoreo().getPunto().getNombrePunto(),
                    primera.getParametro().getIdParametro(), primera.getParametro().getNombreParametro(),
                    primera.getParametro().getUnidadMedida(), ls.size(),
                    suma.divide(BigDecimal.valueOf(ls.size()), 3, RoundingMode.HALF_UP), min, max,
                    alertasPorClave.getOrDefault(g.getKey(), 0)));
        }
        return filas;
    }

    private static String clave(LecturaParametro l) {
        return l.getMonitoreo().getPunto().getNombrePunto() + "|" + l.getParametro().getNombreParametro();
    }

    private ReporteResponse completo(Reporte r) {
        List<ReporteResponse.Detalle> detalles = detalleRepository.deReporte(r.getIdReporte()).stream()
                .map(ReporteResponse.Detalle::desde).toList();
        List<EnvioResponse> envios = envioRepository.deReporte(r.getIdReporte()).stream()
                .map(EnvioResponse::desde).toList();
        return ReporteResponse.desde(r, detalles, envios);
    }

    private Reporte buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Reporte no encontrado: " + id));
    }

    private static String titulo(TipoReporte tipo) {
        return switch (tipo) {
            case AMBIENTAL -> "Reporte ambiental de monitoreo hídrico";
            case AUDITORIA -> "Reporte de auditoría ambiental";
            case ESPECIAL -> "Reporte especial de monitoreo hídrico";
        };
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public record Archivo(String nombre, String tipoContenido, byte[] contenido) {}
}
