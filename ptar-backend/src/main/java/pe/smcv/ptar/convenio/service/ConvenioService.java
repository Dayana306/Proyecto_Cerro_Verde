package pe.smcv.ptar.convenio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.convenio.dto.ConvenioDto;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;
import pe.smcv.ptar.convenio.entity.Convenio;
import pe.smcv.ptar.convenio.entity.TipoMeta;
import pe.smcv.ptar.convenio.repository.ConvenioRepository;
import pe.smcv.ptar.monitoreo.repository.LecturaRepository;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.repository.ParametroRepository;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Convenio hídrico y verificación de su meta de caudal (CU10). */
@Service
public class ConvenioService {

    private static final String ENTIDAD = "CONVENIO";

    private final ConvenioRepository repo;
    private final EntidadExternaService entidadService;
    private final PuntoMonitoreoService puntoService;
    private final ParametroRepository parametroRepository;
    private final LecturaRepository lecturaRepository;
    private final AuditoriaService auditoria;

    public ConvenioService(ConvenioRepository repo, EntidadExternaService entidadService,
                           PuntoMonitoreoService puntoService, ParametroRepository parametroRepository,
                           LecturaRepository lecturaRepository, AuditoriaService auditoria) {
        this.repo = repo;
        this.entidadService = entidadService;
        this.puntoService = puntoService;
        this.parametroRepository = parametroRepository;
        this.lecturaRepository = lecturaRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ConvenioDto.Response> listar(Estado estado) {
        return repo.buscar(estado).stream().map(ConvenioDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public ConvenioDto.Response obtener(Integer id) {
        return ConvenioDto.Response.desde(buscar(id));
    }

    @Transactional
    public ConvenioDto.Response crear(ConvenioDto.Request req) {
        Convenio c = new Convenio();
        aplicar(c, req);
        ConvenioDto.Response r = ConvenioDto.Response.desde(repo.save(c));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idConvenio(), "Convenio creado: " + r.nombre(), null, r);
        return r;
    }

    @Transactional
    public ConvenioDto.Response actualizar(Integer id, ConvenioDto.Request req) {
        Convenio c = buscar(id);
        ConvenioDto.Response antes = ConvenioDto.Response.desde(c);
        aplicar(c, req);
        ConvenioDto.Response r = ConvenioDto.Response.desde(repo.save(c));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Convenio modificado: " + r.nombre(), antes, r);
        return r;
    }

    @Transactional
    public ConvenioDto.Response cambiarEstado(Integer id, Estado estado) {
        Convenio c = buscar(id);
        c.setEstado(estado);
        ConvenioDto.Response r = ConvenioDto.Response.desde(repo.save(c));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id, "Convenio " + c.getNombre() + " -> " + estado,
                null, null);
        return r;
    }

    /** Cumplimiento del año indicado (por defecto, el actual), recortado a la vigencia del convenio. */
    @Transactional(readOnly = true)
    public CumplimientoResponse cumplimiento(Integer id, Integer anio) {
        return calcular(buscar(id), anio == null ? LocalDate.now().getYear() : anio);
    }

    /** Para el dashboard: cumplimiento del año actual del convenio activo más reciente (si existe). */
    @Transactional(readOnly = true)
    public Optional<CumplimientoResponse> cumplimientoVigente() {
        return repo.buscar(Estado.ACTIVO).stream().findFirst()
                .map(c -> calcular(c, LocalDate.now().getYear()));
    }

    private CumplimientoResponse calcular(Convenio c, int anio) {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        if (c.getFechaInicio().isAfter(desde)) {
            desde = c.getFechaInicio();
        }
        if (c.getFechaFin() != null && c.getFechaFin().isBefore(hasta)) {
            hasta = c.getFechaFin();
        }
        LocalDate hoy = LocalDate.now();
        if (hasta.isAfter(hoy)) {
            hasta = hoy; // año en curso: acumulado a la fecha
        }

        BigDecimal meta = c.getCaudalMetaM3s();
        String punto = c.getPunto() == null ? "Todos los puntos" : c.getPunto().getNombrePunto();
        String metodo = "Promedio de los promedios diarios de caudal; cumple si es mayor o igual a la meta";

        Parametro caudal = parametroRepository.findByCodigoIgnoreCase(Parametro.CODIGO_CAUDAL).orElse(null);
        if (caudal == null || desde.isAfter(hasta)) {
            return new CumplimientoResponse(c.getIdConvenio(), c.getNombre(), anio, desde, hasta, punto, meta,
                    null, null, null, 0, 0, metodo, List.of());
        }

        List<CalculoCumplimiento.Medicion> mediciones = lecturaRepository
                .serie(caudal.getIdParametro(), c.getPunto() == null ? null : c.getPunto().getIdPunto(),
                        Fechas.inicio(desde), Fechas.fin(hasta))
                .stream()
                .map(l -> new CalculoCumplimiento.Medicion(l.getMonitoreo().getHoraMuestreo(), l.getValor()))
                .toList();
        CalculoCumplimiento.Resultado res = CalculoCumplimiento.calcular(mediciones);

        List<CumplimientoResponse.Mes> meses = res.meses().stream()
                .map(m -> new CumplimientoResponse.Mes(m.mes(), m.promedio(),
                        m.promedio() == null ? null : m.promedio().compareTo(meta) >= 0,
                        m.diasConDatos(), m.mediciones()))
                .toList();
        Boolean cumple = res.promedio() == null ? null : res.promedio().compareTo(meta) >= 0;
        return new CumplimientoResponse(c.getIdConvenio(), c.getNombre(), anio, desde, hasta, punto, meta,
                res.promedio(), CalculoCumplimiento.porcentaje(res.promedio(), meta), cumple,
                res.diasConDatos(), res.mediciones(), metodo, meses);
    }

    private void aplicar(Convenio c, ConvenioDto.Request req) {
        if (req.fechaFin() != null && req.fechaFin().isBefore(req.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio");
        }
        c.setNombre(req.nombre().trim());
        c.setEntidad(entidadService.buscarActiva(req.idEntidad()));
        c.setPunto(req.idPunto() == null ? null : puntoService.buscarActivo(req.idPunto()));
        c.setFechaInicio(req.fechaInicio());
        c.setFechaFin(req.fechaFin());
        c.setCaudalMetaM3s(req.caudalMetaM3s());
        c.setTipoMeta(req.tipoMeta() == null ? TipoMeta.PROMEDIO_ANUAL : req.tipoMeta());
        c.setObservacion(req.observacion());
    }

    private Convenio buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Convenio no encontrado: " + id));
    }
}
