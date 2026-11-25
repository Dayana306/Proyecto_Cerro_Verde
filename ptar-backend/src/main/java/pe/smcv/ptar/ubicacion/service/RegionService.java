package pe.smcv.ptar.ubicacion.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.ubicacion.dto.RegionDto;
import pe.smcv.ptar.ubicacion.entity.Region;
import pe.smcv.ptar.ubicacion.repository.RegionRepository;

import java.util.List;

@Service
public class RegionService {

    private static final String ENTIDAD = "REGION";
    private static final Sort ORDEN = Sort.by("nombreRegion");

    private final RegionRepository repo;
    private final AuditoriaService auditoria;

    public RegionService(RegionRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<RegionDto.Response> listar(Estado estado) {
        List<Region> datos = estado == null ? repo.findAll(ORDEN) : repo.findByEstado(estado, ORDEN);
        return datos.stream().map(RegionDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public RegionDto.Response obtener(Integer id) {
        return RegionDto.Response.desde(buscar(id));
    }

    @Transactional
    public RegionDto.Response crear(RegionDto.Request req) {
        String nombre = req.nombreRegion().trim();
        if (repo.existsByNombreRegionIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una región con ese nombre");
        }
        Region r = new Region();
        r.setNombreRegion(nombre);
        r.setDescripcion(req.descripcion());
        RegionDto.Response res = RegionDto.Response.desde(repo.save(r));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, res.idRegion(), "Región creada: " + nombre, null, res);
        return res;
    }

    @Transactional
    public RegionDto.Response actualizar(Integer id, RegionDto.Request req) {
        Region r = buscar(id);
        RegionDto.Response antes = RegionDto.Response.desde(r);
        String nombre = req.nombreRegion().trim();
        if (repo.existsByNombreRegionIgnoreCaseAndIdRegionNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe una región con ese nombre");
        }
        r.setNombreRegion(nombre);
        r.setDescripcion(req.descripcion());
        RegionDto.Response res = RegionDto.Response.desde(repo.save(r));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Región modificada: " + nombre, antes, res);
        return res;
    }

    @Transactional
    public RegionDto.Response cambiarEstado(Integer id, Estado estado) {
        Region r = buscar(id);
        r.setEstado(estado);
        RegionDto.Response res = RegionDto.Response.desde(repo.save(r));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Región " + r.getNombreRegion() + " -> " + estado, null, null);
        return res;
    }

    @Transactional(readOnly = true)
    public Region buscarActiva(Integer id) {
        Region r = buscar(id);
        if (r.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("La región seleccionada está inactiva");
        }
        return r;
    }

    private Region buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Región no encontrada: " + id));
    }
}
