package pe.smcv.ptar.parametro.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.parametro.dto.ParametroDto;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.repository.ParametroRepository;

import java.util.List;

@Service
public class ParametroService {

    private static final String ENTIDAD = "PARAMETRO";
    private static final Sort ORDEN = Sort.by("nombreParametro");

    private final ParametroRepository repo;
    private final AuditoriaService auditoria;

    public ParametroService(ParametroRepository repo, AuditoriaService auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ParametroDto.Response> listar(Estado estado) {
        List<Parametro> datos = estado == null ? repo.findAll(ORDEN) : repo.findByEstado(estado, ORDEN);
        return datos.stream().map(ParametroDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public ParametroDto.Response obtener(Integer id) {
        return ParametroDto.Response.desde(buscar(id));
    }

    @Transactional
    public ParametroDto.Response crear(ParametroDto.Request req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (repo.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un parámetro con el código " + codigo);
        }
        Parametro p = new Parametro();
        p.setCodigo(codigo);
        aplicar(p, req);
        ParametroDto.Response r = ParametroDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idParametro(), "Parámetro creado: " + codigo, null, r);
        return r;
    }

    @Transactional
    public ParametroDto.Response actualizar(Integer id, ParametroDto.Request req) {
        Parametro p = buscar(id);
        ParametroDto.Response antes = ParametroDto.Response.desde(p);
        String codigo = req.codigo().trim().toUpperCase();
        if (repo.existsByCodigoIgnoreCaseAndIdParametroNot(codigo, id)) {
            throw new ReglaNegocioException("Ya existe un parámetro con el código " + codigo);
        }
        if (Parametro.CODIGO_CAUDAL.equals(p.getCodigo()) && !Parametro.CODIGO_CAUDAL.equals(codigo)) {
            throw new ReglaNegocioException("El código CAUDAL no se puede cambiar: lo usa el cálculo del convenio");
        }
        p.setCodigo(codigo);
        aplicar(p, req);
        ParametroDto.Response r = ParametroDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Parámetro modificado: " + codigo, antes, r);
        return r;
    }

    @Transactional
    public ParametroDto.Response cambiarEstado(Integer id, Estado estado) {
        Parametro p = buscar(id);
        p.setEstado(estado);
        ParametroDto.Response r = ParametroDto.Response.desde(repo.save(p));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Parámetro " + p.getCodigo() + " -> " + estado, null, null);
        return r;
    }

    @Transactional(readOnly = true)
    public Parametro buscarActivo(Integer id) {
        Parametro p = buscar(id);
        if (p.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("El parámetro " + p.getNombreParametro() + " está inactivo");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public Parametro buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + id));
    }

    private void aplicar(Parametro p, ParametroDto.Request req) {
        if (req.valorMinValido().compareTo(req.valorMaxValido()) > 0) {
            throw new ReglaNegocioException("valorMinValido no puede ser mayor que valorMaxValido");
        }
        p.setNombreParametro(req.nombreParametro().trim());
        p.setDescripcion(req.descripcion());
        p.setUnidadMedida(req.unidadMedida().trim());
        p.setValorMinValido(req.valorMinValido());
        p.setValorMaxValido(req.valorMaxValido());
    }
}
