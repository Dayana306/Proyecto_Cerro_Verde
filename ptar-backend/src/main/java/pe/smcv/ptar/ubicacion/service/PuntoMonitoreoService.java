package pe.smcv.ptar.ubicacion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.ubicacion.dto.PuntoDto;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.ubicacion.repository.PuntoMonitoreoRepository;

import java.util.List;

/** Puntos (estaciones) de monitoreo: base del registro de mediciones (RF-02). */
@Service
public class PuntoMonitoreoService {

    private static final String ENTIDAD = "PUNTO_MONITOREO";

    private final PuntoMonitoreoRepository repo;
    private final ZonaService zonaService;
    private final AreaService areaService;
    private final AuditoriaService auditoria;

    public PuntoMonitoreoService(PuntoMonitoreoRepository repo, ZonaService zonaService, AreaService areaService,
                                 AuditoriaService auditoria) {
        this.repo = repo;
        this.zonaService = zonaService;
        this.areaService = areaService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PuntoDto.Response> listar(Integer idZona, Integer idArea, Estado estado) {
        return repo.buscar(idZona, idArea, estado).stream().map(PuntoDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public PuntoDto.Response obtener(Integer id) {
        return PuntoDto.Response.desde(buscar(id));
    }

    @Transactional
    public PuntoDto.Response crear(PuntoDto.Request req) {
        String nombre = req.nombrePunto().trim();
        if (repo.existsByNombrePuntoIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe un punto de monitoreo con ese nombre");
        }
        PuntoMonitoreo p = new PuntoMonitoreo();
        aplicar(p, req, nombre);
        PuntoDto.Response r = PuntoDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idPunto(), "Punto creado: " + nombre, null, r);
        return r;
    }

    @Transactional
    public PuntoDto.Response actualizar(Integer id, PuntoDto.Request req) {
        PuntoMonitoreo p = buscar(id);
        PuntoDto.Response antes = PuntoDto.Response.desde(p);
        String nombre = req.nombrePunto().trim();
        if (repo.existsByNombrePuntoIgnoreCaseAndIdPuntoNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe un punto de monitoreo con ese nombre");
        }
        aplicar(p, req, nombre);
        PuntoDto.Response r = PuntoDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Punto modificado: " + nombre, antes, r);
        return r;
    }

    @Transactional
    public PuntoDto.Response cambiarEstado(Integer id, Estado estado) {
        PuntoMonitoreo p = buscar(id);
        p.setEstado(estado);
        PuntoDto.Response r = PuntoDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Punto " + p.getNombrePunto() + " -> " + estado, null, null);
        return r;
    }

    /** Para otros servicios (monitoreo, umbral, reporte): el punto debe existir y estar activo. */
    @Transactional(readOnly = true)
    public PuntoMonitoreo buscarActivo(Integer id) {
        PuntoMonitoreo p = buscar(id);
        if (p.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("El punto de monitoreo seleccionado está inactivo");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public PuntoMonitoreo buscar(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Punto de monitoreo no encontrado: " + id));
    }

    private void aplicar(PuntoMonitoreo p, PuntoDto.Request req, String nombre) {
        p.setNombrePunto(nombre);
        p.setDescripcion(req.descripcion());
        p.setLatitud(req.latitud());
        p.setLongitud(req.longitud());
        p.setZona(zonaService.buscarActiva(req.idZona()));
        p.setArea(areaService.buscarActiva(req.idArea()));
    }
}
