package pe.smcv.ptar.ubicacion.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.ubicacion.dto.AreaDto;
import pe.smcv.ptar.ubicacion.entity.Area;
import pe.smcv.ptar.ubicacion.repository.AreaRepository;

import java.util.List;

@Service
public class AreaService {

    private static final String ENTIDAD = "AREA";
    private static final Sort ORDEN = Sort.by("nombreArea");

    private final AreaRepository repo;
    private final AuditoriaService auditoria;

    public AreaService(AreaRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<AreaDto.Response> listar(Estado estado) {
        List<Area> datos = estado == null ? repo.findAll(ORDEN) : repo.findByEstado(estado, ORDEN);
        return datos.stream().map(AreaDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public AreaDto.Response obtener(Integer id) {
        return AreaDto.Response.desde(buscar(id));
    }

    @Transactional
    public AreaDto.Response crear(AreaDto.Request req) {
        String nombre = req.nombreArea().trim();
        if (repo.existsByNombreAreaIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe un área con ese nombre");
        }
        Area a = new Area();
        a.setNombreArea(nombre);
        a.setDescripcion(req.descripcion());
        AreaDto.Response r = AreaDto.Response.desde(repo.save(a));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idArea(), "Área creada: " + nombre, null, r);
        return r;
    }

    @Transactional
    public AreaDto.Response actualizar(Integer id, AreaDto.Request req) {
        Area a = buscar(id);
        AreaDto.Response antes = AreaDto.Response.desde(a);
        String nombre = req.nombreArea().trim();
        if (repo.existsByNombreAreaIgnoreCaseAndIdAreaNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe un área con ese nombre");
        }
        a.setNombreArea(nombre);
        a.setDescripcion(req.descripcion());
        AreaDto.Response r = AreaDto.Response.desde(repo.save(a));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Área modificada: " + nombre, antes, r);
        return r;
    }

    @Transactional
    public AreaDto.Response cambiarEstado(Integer id, Estado estado) {
        Area a = buscar(id);
        a.setEstado(estado);
        AreaDto.Response r = AreaDto.Response.desde(repo.save(a));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Área " + a.getNombreArea() + " -> " + estado, null, null);
        return r;
    }

    /** Para otros servicios: el área debe existir y estar activa. */
    @Transactional(readOnly = true)
    public Area buscarActiva(Integer id) {
        Area a = buscar(id);
        if (a.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("El área seleccionada está inactiva");
        }
        return a;
    }

    private Area buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Área no encontrada: " + id));
    }
}
