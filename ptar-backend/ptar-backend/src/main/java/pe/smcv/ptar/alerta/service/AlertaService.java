package pe.smcv.ptar.alerta.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.alerta.dto.SeguimientoRequest;
import pe.smcv.ptar.alerta.dto.SeguimientoResponse;
import pe.smcv.ptar.alerta.entity.*;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.alerta.repository.NotificacionRepository;
import pe.smcv.ptar.alerta.repository.SeguimientoRepository;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.enums.RolNombre;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.entity.Umbral;
import pe.smcv.ptar.parametro.service.EvaluadorUmbral;
import pe.smcv.ptar.parametro.service.UmbralService;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Generación automática de alertas (RF-05, CU09) y su seguimiento.
 * Se invoca desde MonitoreoService dentro de la misma transacción del registro.
 */
@Service
public class AlertaService {

    private static final String ENTIDAD = "ALERTA";
    /** Quién recibe la notificación en el panel (postcondición de RF-05). */
    private static final List<String> ROLES_NOTIFICADOS =
            List.of(RolNombre.GERENTE.name(), RolNombre.ADMINISTRADOR.name());

    private final AlertaRepository repo;
    private final NotificacionRepository notificacionRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final UmbralService umbralService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public AlertaService(AlertaRepository repo, NotificacionRepository notificacionRepository,
                         SeguimientoRepository seguimientoRepository, UmbralService umbralService,
                         UsuarioRepository usuarioRepository, AuditoriaService auditoria) {
        this.repo = repo;
        this.notificacionRepository = notificacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.umbralService = umbralService;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    /**
     * Evalúa una lectura contra su umbral vigente y, si lo excede, crea la alerta y notifica.
     * No duplica: si la lectura ya tiene una alerta sin cerrar, no crea otra.
     *
     * @param fechaDeteccion null = ahora (uso normal). Los datos de demostración pasan la hora del muestreo.
     */
    @Transactional
    public Optional<Alerta> evaluar(LecturaParametro lectura, LocalDateTime fechaDeteccion) {
        if (lectura.getEstado() != Estado.ACTIVO || lectura.getMonitoreo().getEstado() != Estado.ACTIVO) {
            return Optional.empty();
        }
        Integer idPunto = lectura.getMonitoreo().getPunto().getIdPunto();
        Optional<Umbral> umbral = umbralService.vigente(lectura.getParametro().getIdParametro(), idPunto);
        if (umbral.isEmpty()) {
            return Optional.empty();
        }
        if (lectura.getIdLectura() != null && repo.contarNoCerradasDeLectura(lectura.getIdLectura()) > 0) {
            return Optional.empty();
        }
        Umbral u = umbral.get();
        Optional<EvaluadorUmbral.Resultado> resultado = EvaluadorUmbral.evaluar(lectura.getValor(),
                u.getLimiteMin(), u.getLimiteMax(), u.getCriticoMin(), u.getCriticoMax());
        if (resultado.isEmpty()) {
            return Optional.empty();
        }

        Alerta a = new Alerta();
        a.setLectura(lectura);
        a.setUmbral(u);
        a.setNivel(NivelAlerta.valueOf(resultado.get().nivel().name()));
        a.setTipoAlerta(TipoAlerta.valueOf(resultado.get().tipo().name()));
        a.setValorDetectado(lectura.getValor());
        a.setFechaDeteccion(fechaDeteccion != null ? fechaDeteccion : LocalDateTime.now());
        Alerta guardada = repo.save(a);
        notificar(guardada, lectura);
        return Optional.of(guardada);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertaResponse> buscar(EstadoAlerta estado, NivelAlerta nivel, Integer idPunto,
                                               Integer idParametro, LocalDate desde, LocalDate hasta,
                                               Pageable pageable) {
        return PageResponse.from(repo.buscar(estado, nivel, idPunto, idParametro,
                Fechas.inicio(desde), Fechas.fin(hasta), pageable), AlertaResponse::desde);
    }

    @Transactional(readOnly = true)
    public AlertaResponse obtener(Integer id) {
        Alerta a = buscarCompleta(id);
        List<SeguimientoResponse> seguimientos = seguimientoRepository.deAlerta(id).stream()
                .map(SeguimientoResponse::desde).toList();
        return AlertaResponse.desde(a, seguimientos);
    }

    /** Registra una acción sobre la alerta y, opcionalmente, cambia su estado. */
    @Transactional
    public AlertaResponse agregarSeguimiento(Integer id, SeguimientoRequest req, Integer idUsuario) {
        Alerta a = buscarCompleta(id);
        EstadoAlerta anterior = a.getEstado();
        if (anterior == EstadoAlerta.CERRADA) {
            throw new ReglaNegocioException("La alerta está cerrada y no admite más seguimiento");
        }
        EstadoAlerta nuevo = req.nuevoEstado() != null ? req.nuevoEstado() : anterior;
        guardarSeguimiento(a, req.comentario().trim(), anterior, nuevo, usuarioRepository.getReferenceById(idUsuario));
        a.setEstado(nuevo);
        repo.save(a);
        auditoria.registrar(anterior == nuevo ? AccionAuditoria.MODIFICAR : AccionAuditoria.CAMBIAR_ESTADO,
                ENTIDAD, id, "Seguimiento de alerta #" + id + " (" + anterior + " -> " + nuevo + ")", null,
                req.comentario());
        return obtener(id);
    }

    /** Al corregir el valor de una lectura, deja constancia en sus alertas abiertas. */
    @Transactional
    public void registrarCorreccion(LecturaParametro lectura, BigDecimal valorAnterior, Integer idUsuario) {
        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        for (Alerta a : repo.noCerradasDeLectura(lectura.getIdLectura())) {
            guardarSeguimiento(a, "Valor de la lectura corregido de " + texto(valorAnterior) + " a "
                    + texto(lectura.getValor()) + ". Revise si la alerta sigue vigente.",
                    a.getEstado(), a.getEstado(), usuario);
        }
    }

    /** Al anular un monitoreo, sus alertas sin cerrar se cierran automáticamente. */
    @Transactional
    public int cerrarPorAnulacion(Integer idMonitoreo, Integer idUsuario) {
        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        int cerradas = 0;
        for (Alerta a : repo.deMonitoreo(idMonitoreo)) {
            if (a.getEstado() != EstadoAlerta.CERRADA) {
                guardarSeguimiento(a, "Cerrada automáticamente: el monitoreo #" + idMonitoreo + " fue anulado.",
                        a.getEstado(), EstadoAlerta.CERRADA, usuario);
                a.setEstado(EstadoAlerta.CERRADA);
                repo.save(a);
                cerradas++;
            }
        }
        return cerradas;
    }

    private void notificar(Alerta a, LecturaParametro lectura) {
        Parametro p = lectura.getParametro();
        String mensaje = String.format("Alerta %s: %s = %s %s en %s",
                a.getNivel(), p.getNombreParametro(), texto(lectura.getValor()), p.getUnidadMedida(),
                lectura.getMonitoreo().getPunto().getNombrePunto());
        if (mensaje.length() > 255) {
            mensaje = mensaje.substring(0, 255);
        }
        for (Usuario destinatario : usuarioRepository.findByRol_NombreInAndEstado(ROLES_NOTIFICADOS, Estado.ACTIVO)) {
            Notificacion n = new Notificacion();
            n.setAlerta(a);
            n.setUsuario(destinatario);
            n.setMensaje(mensaje);
            n.setCanal(CanalNotificacion.PANEL);
            notificacionRepository.save(n);
        }
    }

    private void guardarSeguimiento(Alerta a, String comentario, EstadoAlerta anterior, EstadoAlerta nuevo,
                                    Usuario usuario) {
        Seguimiento s = new Seguimiento();
        s.setAlerta(a);
        s.setComentario(comentario.length() > 500 ? comentario.substring(0, 500) : comentario);
        s.setEstadoAnterior(anterior);
        s.setEstadoNuevo(nuevo);
        s.setUsuario(usuario);
        seguimientoRepository.save(s);
    }

    private Alerta buscarCompleta(Integer id) {
        return repo.buscarCompleta(id).orElseThrow(() -> new NotFoundException("Alerta no encontrada: " + id));
    }

    private static String texto(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }
}
