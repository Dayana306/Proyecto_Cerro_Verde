package pe.smcv.ptar.parametro.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.parametro.dto.UmbralDto;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.entity.Umbral;
import pe.smcv.ptar.parametro.repository.UmbralRepository;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Configuración de umbrales (RF-06). Regla de versionado: solo hay UN umbral activo por
 * (parámetro, punto). Crear o modificar desactiva el anterior y guarda uno nuevo, así cada
 * alerta conserva el umbral exacto con el que se evaluó.
 */
@Service
public class UmbralService {

    private static final String ENTIDAD = "UMBRAL";

    private final UmbralRepository repo;
    private final ParametroService parametroService;
    private final PuntoMonitoreoService puntoService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public UmbralService(UmbralRepository repo, ParametroService parametroService,
                         PuntoMonitoreoService puntoService, UsuarioRepository usuarioRepository,
                         AuditoriaService auditoria) {
        this.repo = repo;
        this.parametroService = parametroService;
        this.puntoService = puntoService;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    /** soloGenerales=true devuelve solo los umbrales sin punto. */
    @Transactional(readOnly = true)
    public List<UmbralDto.Response> listar(Integer idParametro, Integer idPunto, boolean soloGenerales, Estado estado) {
        return repo.buscar(idParametro, idPunto, soloGenerales, estado).stream()
                .map(UmbralDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public UmbralDto.Response obtener(Integer id) {
        return UmbralDto.Response.desde(buscar(id));
    }

    @Transactional
    public UmbralDto.Response crear(UmbralDto.Request req, Integer idUsuario) {
        Parametro parametro = parametroService.buscarActivo(req.idParametro());
        PuntoMonitoreo punto = req.idPunto() == null ? null : puntoService.buscarActivo(req.idPunto());
        return guardarNuevaVersion(parametro, punto, req.limiteMin(), req.limiteMax(),
                req.criticoMin(), req.criticoMax(), idUsuario);
    }

    /** "Modificar" = nueva versión con los mismos parámetro y punto. */
    @Transactional
    public UmbralDto.Response actualizar(Integer id, UmbralDto.LimitesRequest req, Integer idUsuario) {
        Umbral actual = buscar(id);
        if (actual.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("Solo se puede modificar un umbral activo");
        }
        return guardarNuevaVersion(actual.getParametro(), actual.getPunto(), req.limiteMin(), req.limiteMax(),
                req.criticoMin(), req.criticoMax(), idUsuario);
    }

    /** Desactivar sin reemplazo: el parámetro deja de generar alertas en ese punto (o usa el general). */
    @Transactional
    public UmbralDto.Response cambiarEstado(Integer id, Estado estado) {
        Umbral u = buscar(id);
        if (estado == Estado.ACTIVO && u.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("Un umbral desactivado no se reactiva: cree uno nuevo");
        }
        u.setEstado(estado);
        UmbralDto.Response r = UmbralDto.Response.desde(repo.save(u));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Umbral de " + r.parametro() + " (" + r.punto() + ") -> " + estado, null, null);
        return r;
    }

    /**
     * Umbral que aplica a una lectura: primero el específico del punto; si no hay, el general.
     * Lo usa el módulo de alertas.
     */
    @Transactional(readOnly = true)
    public Optional<Umbral> vigente(Integer idParametro, Integer idPunto) {
        if (idPunto != null) {
            List<Umbral> especificos = repo.activosDePunto(idParametro, idPunto);
            if (!especificos.isEmpty()) {
                return Optional.of(especificos.get(0));
            }
        }
        return repo.activosGenerales(idParametro).stream().findFirst();
    }

    private UmbralDto.Response guardarNuevaVersion(Parametro parametro, PuntoMonitoreo punto,
                                                   BigDecimal limiteMin, BigDecimal limiteMax,
                                                   BigDecimal criticoMin, BigDecimal criticoMax,
                                                   Integer idUsuario) {
        String error = EvaluadorUmbral.validar(limiteMin, limiteMax, criticoMin, criticoMax);
        if (error != null) {
            throw new ReglaNegocioException(error);
        }

        Integer idPunto = punto == null ? null : punto.getIdPunto();
        List<Umbral> anteriores = idPunto == null
                ? repo.activosGenerales(parametro.getIdParametro())
                : repo.activosDePunto(parametro.getIdParametro(), idPunto);
        UmbralDto.Response antes = anteriores.isEmpty() ? null : UmbralDto.Response.desde(anteriores.get(0));
        anteriores.forEach(a -> a.setEstado(Estado.INACTIVO));
        repo.saveAll(anteriores);

        Umbral u = new Umbral();
        u.setParametro(parametro);
        u.setPunto(punto);
        u.setLimiteMin(limiteMin);
        u.setLimiteMax(limiteMax);
        u.setCriticoMin(criticoMin);
        u.setCriticoMax(criticoMax);
        u.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        Umbral guardado = repo.save(u);
        // Recarga con relaciones para armar la respuesta (nombre de quien configuró).
        UmbralDto.Response r = UmbralDto.Response.desde(buscar(guardado.getIdUmbral()));
        auditoria.registrar(antes == null ? AccionAuditoria.CREAR : AccionAuditoria.MODIFICAR, ENTIDAD,
                r.idUmbral(), "Umbral de " + r.parametro() + " (" + r.punto() + ")", antes, r);
        return r;
    }

    private Umbral buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Umbral no encontrado: " + id));
    }
}
