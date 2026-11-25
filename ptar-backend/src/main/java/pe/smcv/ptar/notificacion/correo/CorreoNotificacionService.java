package pe.smcv.ptar.notificacion.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CorreoNotificacionService {
    private static final Logger log = LoggerFactory.getLogger(CorreoNotificacionService.class);
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_FILES = 5;
    private static final int MAX_RECIPIENTS = 30;
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$", Pattern.CASE_INSENSITIVE);

    private final CorreoNotificacionRepository repo;
    private final JakartaMailService mail;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public CorreoNotificacionService(CorreoNotificacionRepository repo, JakartaMailService mail,
                                     UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.repo = repo;
        this.mail = mail;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<CorreoNotificacionResponse> listar(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable), CorreoNotificacionResponse::desde);
    }

    @Transactional
    public CorreoNotificacionResponse enviar(String destinatariosRaw, String asuntoRaw, String mensajeRaw,
                                              boolean html, List<MultipartFile> adjuntos, Integer idUsuario) {
        List<String> destinatarios = normalizarDestinatarios(destinatariosRaw);
        String asunto = asuntoRaw == null ? "" : asuntoRaw.trim();
        String mensaje = mensajeRaw == null ? "" : mensajeRaw.trim();
        if (asunto.isBlank() || asunto.length() > 180) {
            throw new ReglaNegocioException("El asunto es obligatorio y admite hasta 180 caracteres.");
        }
        if (mensaje.isBlank() || mensaje.length() > 20000) {
            throw new ReglaNegocioException("El mensaje es obligatorio y admite hasta 20 000 caracteres.");
        }
        List<MultipartFile> files = adjuntos == null ? List.of() : adjuntos.stream()
                .filter(file -> file != null && !file.isEmpty()).toList();
        if (files.size() > MAX_FILES) throw new ReglaNegocioException("Puedes adjuntar como máximo 5 archivos por correo.");
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new ReglaNegocioException("El archivo " + safeName(file) + " supera el límite de 10 MB.");
            }
        }

        CorreoNotificacion registro = new CorreoNotificacion();
        registro.setDestinatarios(String.join(", ", destinatarios));
        registro.setAsunto(asunto);
        registro.setMensaje(mensaje);
        registro.setHtml(html);
        registro.setNombresAdjuntos(files.isEmpty() ? null : files.stream().map(CorreoNotificacionService::safeName).reduce((a,b) -> a + ", " + b).orElse(null));
        registro.setEstado("PENDIENTE");
        if (idUsuario != null) {
            Usuario usuario = usuarios.getReferenceById(idUsuario);
            registro.setUsuario(usuario);
        }
        repo.saveAndFlush(registro);

        String detalle;
        try {
            mail.enviar(destinatarios, asunto, mensaje, html, files);
            registro.setEstado("ENVIADO");
            registro.setErrorResumen(null);
            detalle = "Correo enviado correctamente a " + destinatarios.size() + " destinatario(s).";
        } catch (Exception error) {
            String mensajeError = mensajeSeguro(error);
            registro.setEstado("ERROR");
            registro.setErrorResumen(mensajeError);
            detalle = "No se pudo enviar el correo: " + mensajeError;
            log.error("Fallo el envío de correo #{} a través de SMTP", registro.getIdCorreo(), error);
        }
        repo.save(registro);
        auditoria.registrarComo(idUsuario, AccionAuditoria.CREAR, "CORREO_NOTIFICACION", registro.getIdCorreo(),
                detalle, null, new EstadoEnvio(registro.getEstado(), registro.getDestinatarios(), registro.getAsunto()));
        return CorreoNotificacionResponse.desde(registro);
    }

    private static List<String> normalizarDestinatarios(String raw) {
        if (raw == null || raw.isBlank()) throw new ReglaNegocioException("Ingresa al menos un correo destinatario.");
        Set<String> unique = new LinkedHashSet<>();
        for (String token : raw.split("[,;\\s]+")) {
            String email = token.trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty()) continue;
            if (!EMAIL.matcher(email).matches()) throw new ReglaNegocioException("El correo destinatario no tiene un formato válido: " + email);
            unique.add(email);
        }
        if (unique.isEmpty()) throw new ReglaNegocioException("Ingresa al menos un correo destinatario.");
        if (unique.size() > MAX_RECIPIENTS) throw new ReglaNegocioException("Puedes enviar un correo a un máximo de 30 destinatarios.");
        return new ArrayList<>(unique);
    }

    private static String safeName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "adjunto";
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_").trim();
        return name.isBlank() ? "adjunto" : name;
    }

    private static String mensajeSeguro(Exception error) {
        String cause = error.getMessage();
        if (cause == null || cause.isBlank()) return "Revisa la configuración SMTP, la autenticación y la conexión de red.";
        cause = cause.replaceAll("[\\r\\n\\t]", " ").replaceAll("(?i)(password|passwd|token)=\\S+", "$1=[oculto]").trim();
        if (cause.length() > 480) cause = cause.substring(0, 480);
        return cause;
    }

    private record EstadoEnvio(String estado, String destinatarios, String asunto) {}
}
