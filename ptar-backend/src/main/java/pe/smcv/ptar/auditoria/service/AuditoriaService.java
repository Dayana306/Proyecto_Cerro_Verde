package pe.smcv.ptar.auditoria.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.entity.Historial;
import pe.smcv.ptar.auditoria.repository.HistorialRepository;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.security.SesionActual;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Bitácora de auditoría (RF-10, RNF-04). Los servicios la llaman explícitamente
 * en cada operación de escritura; se guarda en la MISMA transacción que la operación,
 * así no queda rastro de operaciones que fallaron.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int MAX_DESCRIPCION = 255;

    private final HistorialRepository repo;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(HistorialRepository repo, UsuarioRepository usuarioRepository, ObjectMapper objectMapper) {
        this.repo = repo;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /** Registra usando el usuario autenticado de la petición actual. */
    @Transactional
    public void registrar(AccionAuditoria accion, String entidad, Number idEntidad, String descripcion,
                         Object antes, Object despues) {
        registrarComo(SesionActual.idUsuarioOrNull(), accion, entidad, idEntidad, descripcion, antes, despues);
    }

    /** Registra indicando el usuario explícitamente (login, procesos sin sesión). idUsuario puede ser null. */
    @Transactional
    public void registrarComo(Integer idUsuario, AccionAuditoria accion, String entidad, Number idEntidad,
                             String descripcion, Object antes, Object despues) {
        Historial h = new Historial();
        h.setAccion(accion);
        h.setEntidad(entidad);
        h.setIdEntidad(idEntidad == null ? null : idEntidad.longValue());
        h.setDescripcion(recortar(descripcion));
        h.setDatosAnteriores(aJson(antes));
        h.setDatosNuevos(aJson(despues));
        h.setIpOrigen(ipActual());
        if (idUsuario != null) {
            h.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        }
        repo.save(h);
    }

    @Transactional(readOnly = true)
    public PageResponse<HistorialResponse> buscar(String entidad, AccionAuditoria accion, Integer idUsuario,
                                                  LocalDate desde, LocalDate hasta, Pageable pageable) {
        return PageResponse.from(
                repo.buscar(entidad, accion, idUsuario, Fechas.inicio(desde), Fechas.fin(hasta), pageable),
                HistorialResponse::desde);
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> listar(String entidad, AccionAuditoria accion, Integer idUsuario,
                                          LocalDate desde, LocalDate hasta) {
        return repo.listar(entidad, accion, idUsuario, Fechas.inicio(desde), Fechas.fin(hasta)).stream()
                .map(HistorialResponse::desde)
                .toList();
    }

    private String aJson(Object datos) {
        if (datos == null) {
            return null;
        }
        if (datos instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar datos de auditoría: {}", e.getMessage());
            return null;
        }
    }

    private static String recortar(String texto) {
        if (texto == null || texto.length() <= MAX_DESCRIPCION) {
            return texto;
        }
        return texto.substring(0, MAX_DESCRIPCION);
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return null;
    }
}
