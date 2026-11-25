package pe.smcv.ptar.notificacion.correo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.util.List;

/** Envío manual de notificaciones por email. Restringido al rol Administrador. */
@RestController
@RequestMapping("/api/notificaciones/correos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CorreoNotificacionController {
    private final CorreoNotificacionService service;
    private final JakartaMailService mail;

    public CorreoNotificacionController(CorreoNotificacionService service, JakartaMailService mail) {
        this.service = service;
        this.mail = mail;
    }

    @GetMapping
    public PageResponse<CorreoNotificacionResponse> listar(
            @PageableDefault(size = 30, sort = "fechaEnvio", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/configuracion")
    public JakartaMailService.MailStatus configuracion() { return mail.status(); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CorreoNotificacionResponse enviar(
            @RequestParam("destinatarios") String destinatarios,
            @RequestParam("asunto") String asunto,
            @RequestParam("mensaje") String mensaje,
            @RequestParam(name = "html", defaultValue = "false") boolean html,
            @RequestParam(name = "adjuntos", required = false) List<MultipartFile> adjuntos,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.enviar(destinatarios, asunto, mensaje, html, adjuntos, usuario.getIdUsuario());
    }
}
