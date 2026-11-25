package pe.smcv.ptar.alerta.publicada;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.util.List;

@Service
public class AlertaPublicadaService {
    private final AlertaPublicadaRepository alertas;
    private final AlertaPublicadaLecturaRepository lecturas;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public AlertaPublicadaService(AlertaPublicadaRepository alertas,
                                  AlertaPublicadaLecturaRepository lecturas,
                                  UsuarioRepository usuarios,
                                  AuditoriaService auditoria) {
        this.alertas = alertas;
        this.lecturas = lecturas;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<AlertaPublicadaResponse> listar(Integer idUsuario) {
        return alertas.findAllByOrderByFechaCreacionDesc().stream()
                .map(a -> AlertaPublicadaResponse.desde(a,
                        lecturas.findByAlerta_IdAlertaPublicadaAndUsuario_IdUsuario(a.getIdAlertaPublicada(), idUsuario).orElse(null)))
                .toList();
    }

    @Transactional
    public AlertaPublicadaResponse crear(AlertaPublicadaRequest request, Integer idUsuario) {
        AlertaPublicada alerta = new AlertaPublicada();
        alerta.setTitulo(request.titulo().trim());
        alerta.setDescripcion(request.descripcion().trim());
        alerta.setNivel(request.nivel());
        alerta.setArea(request.area() == null || request.area().isBlank() ? null : request.area().trim());
        alerta.setCreador(usuarios.getReferenceById(idUsuario));
        AlertaPublicada guardada = alertas.save(alerta);
        auditoria.registrar(AccionAuditoria.CREAR, "ALERTA_PUBLICADA", guardada.getIdAlertaPublicada(),
                "Alerta publicada: " + guardada.getTitulo(), null, guardada.getTitulo());
        return AlertaPublicadaResponse.desde(guardada, null);
    }

    @Transactional
    public AlertaPublicadaResponse actualizar(Integer idAlerta, AlertaPublicadaRequest request, Integer idUsuario) {
        AlertaPublicada alerta = alertas.findById(idAlerta)
                .orElseThrow(() -> new NotFoundException("Alerta publicada no encontrada: " + idAlerta));
        String tituloAnterior = alerta.getTitulo();
        String nivelAnterior = alerta.getNivel().name();
        alerta.setTitulo(request.titulo().trim());
        alerta.setDescripcion(request.descripcion().trim());
        alerta.setNivel(request.nivel());
        alerta.setArea(request.area() == null || request.area().isBlank() ? null : request.area().trim());
        AlertaPublicada actualizada = alertas.save(alerta);
        auditoria.registrar(AccionAuditoria.MODIFICAR, "ALERTA_PUBLICADA", actualizada.getIdAlertaPublicada(),
                "Alerta publicada actualizada: " + actualizada.getTitulo(),
                tituloAnterior + " | prioridad " + nivelAnterior,
                actualizada.getTitulo() + " | prioridad " + actualizada.getNivel().name());
        AlertaPublicadaLectura lectura = lecturas
                .findByAlerta_IdAlertaPublicadaAndUsuario_IdUsuario(idAlerta, idUsuario).orElse(null);
        return AlertaPublicadaResponse.desde(actualizada, lectura);
    }

    @Transactional
    public AlertaPublicadaResponse marcarVista(Integer idAlerta, Integer idUsuario) {
        AlertaPublicada alerta = alertas.findById(idAlerta)
                .orElseThrow(() -> new NotFoundException("Alerta publicada no encontrada: " + idAlerta));
        AlertaPublicadaLectura lectura = lecturas
                .findByAlerta_IdAlertaPublicadaAndUsuario_IdUsuario(idAlerta, idUsuario)
                .orElseGet(() -> {
                    AlertaPublicadaLectura nueva = new AlertaPublicadaLectura();
                    nueva.setAlerta(alerta);
                    nueva.setUsuario(usuarios.getReferenceById(idUsuario));
                    return lecturas.save(nueva);
                });
        return AlertaPublicadaResponse.desde(alerta, lectura);
    }
}
