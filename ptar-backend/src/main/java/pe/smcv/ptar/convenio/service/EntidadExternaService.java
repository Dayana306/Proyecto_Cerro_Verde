package pe.smcv.ptar.convenio.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.convenio.dto.EntidadExternaDto;
import pe.smcv.ptar.convenio.entity.EntidadExterna;
import pe.smcv.ptar.convenio.repository.EntidadExternaRepository;

import java.util.List;

@Service
public class EntidadExternaService {

    private static final String ENTIDAD = "ENTIDAD_EXTERNA";
    private static final Sort ORDEN = Sort.by("nombre");

    private final EntidadExternaRepository repo;
    private final AuditoriaService auditoria;

    public EntidadExternaService(EntidadExternaRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<EntidadExternaDto.Response> listar(Estado estado) {
        List<EntidadExterna> datos = estado == null ? repo.findAll(ORDEN) : repo.findByEstado(estado, ORDEN);
        return datos.stream().map(EntidadExternaDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public EntidadExternaDto.Response obtener(Integer id) {
        return EntidadExternaDto.Response.desde(buscar(id));
    }

    @Transactional
    public EntidadExternaDto.Response crear(EntidadExternaDto.Request req) {
        String nombre = req.nombre().trim();
        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una entidad con ese nombre");
        }
        EntidadExterna e = new EntidadExterna();
        aplicar(e, req, nombre);
        EntidadExternaDto.Response r = EntidadExternaDto.Response.desde(repo.save(e));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idEntidad(), "Entidad creada: " + nombre, null, r);
        return r;
    }

    @Transactional
    public EntidadExternaDto.Response actualizar(Integer id, EntidadExternaDto.Request req) {
        EntidadExterna e = buscar(id);
        EntidadExternaDto.Response antes = EntidadExternaDto.Response.desde(e);
        String nombre = req.nombre().trim();
        if (repo.existsByNombreIgnoreCaseAndIdEntidadNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe una entidad con ese nombre");
        }
        aplicar(e, req, nombre);
        EntidadExternaDto.Response r = EntidadExternaDto.Response.desde(repo.save(e));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Entidad modificada: " + nombre, antes, r);
        return r;
    }

    @Transactional
    public EntidadExternaDto.Response cambiarEstado(Integer id, Estado estado) {
        EntidadExterna e = buscar(id);
        e.setEstado(estado);
        EntidadExternaDto.Response r = EntidadExternaDto.Response.desde(repo.save(e));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id, "Entidad " + e.getNombre() + " -> " + estado,
                null, null);
        return r;
    }

    @Transactional(readOnly = true)
    public EntidadExterna buscarActiva(Integer id) {
        EntidadExterna e = buscar(id);
        if (e.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("La entidad seleccionada está inactiva");
        }
        return e;
    }

    private void aplicar(EntidadExterna e, EntidadExternaDto.Request req, String nombre) {
        e.setNombre(nombre);
        e.setTipoEntidad(req.tipoEntidad());
        e.setCorreo(req.correo() == null || req.correo().isBlank() ? null : req.correo().trim());
        e.setTelefono(req.telefono());
    }

    private EntidadExterna buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Entidad externa no encontrada: " + id));
    }
}
