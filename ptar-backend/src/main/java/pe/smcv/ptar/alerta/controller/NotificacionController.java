package pe.smcv.ptar.alerta.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.alerta.dto.NotificacionResponse;
import pe.smcv.ptar.alerta.service.NotificacionService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.util.Map;

/** Notificaciones del usuario autenticado. Cada usuario solo ve las suyas. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificacionResponse> mias(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                   @RequestParam(defaultValue = "false") boolean soloNoLeidas,
                                                   @PageableDefault(size = 20, sort = "fechaEnvio", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.mias(usuario.getIdUsuario(), soloNoLeidas, pageable);
    }

    /** Para el contador de la campana: {"total": n}. Consultar cada 30-60 s. */
    @GetMapping("/no-leidas/total")
    public Map<String, Long> totalNoLeidas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return Map.of("total", service.noLeidas(usuario.getIdUsuario()));
    }

    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@PathVariable Integer id, @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.marcarLeida(id, usuario.getIdUsuario());
    }

    @PatchMapping("/leidas")
    public Map<String, Integer> marcarTodas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return Map.of("actualizadas", service.marcarTodasLeidas(usuario.getIdUsuario()));
    }
}
