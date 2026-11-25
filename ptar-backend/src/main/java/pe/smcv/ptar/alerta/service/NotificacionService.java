package pe.smcv.ptar.alerta.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.dto.NotificacionResponse;
import pe.smcv.ptar.alerta.entity.Notificacion;
import pe.smcv.ptar.alerta.repository.NotificacionRepository;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.exception.NotFoundException;

import java.time.LocalDateTime;

/** Notificaciones del usuario autenticado (campana del panel). */
@Service
public class NotificacionService {

    private final NotificacionRepository repo;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> mias(Integer idUsuario, boolean soloNoLeidas, Pageable pageable) {
        return PageResponse.from(repo.deUsuario(idUsuario, soloNoLeidas, pageable), NotificacionResponse::desde);
    }

    @Transactional(readOnly = true)
    public long noLeidas(Integer idUsuario) {
        return repo.countByUsuario_IdUsuarioAndFechaLecturaIsNull(idUsuario);
    }

    @Transactional
    public NotificacionResponse marcarLeida(Integer idNotificacion, Integer idUsuario) {
        Notificacion n = repo.findById(idNotificacion)
                .filter(x -> x.getUsuario().getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new NotFoundException("Notificación no encontrada: " + idNotificacion));
        if (n.getFechaLectura() == null) {
            n.setFechaLectura(LocalDateTime.now());
            repo.save(n);
        }
        return NotificacionResponse.desde(n);
    }

    @Transactional
    public int marcarTodasLeidas(Integer idUsuario) {
        return repo.marcarTodasLeidas(idUsuario, LocalDateTime.now());
    }
}
