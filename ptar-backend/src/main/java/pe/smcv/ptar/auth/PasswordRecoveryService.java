package pe.smcv.ptar.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auth.entity.PasswordResetToken;
import pe.smcv.ptar.auth.repository.PasswordResetTokenRepository;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import pe.smcv.ptar.notificacion.correo.JakartaMailService;

/** Recuperación de contraseña con código de un solo uso enviado por correo. */
@Service
public class PasswordRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final int CODE_DIGITS = 8;
    private static final int CODE_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final UsuarioRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final JakartaMailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordRecoveryService(UsuarioRepository usuarios, PasswordResetTokenRepository tokens,
                                   PasswordEncoder encoder, AuditoriaService auditoria,
                                   JakartaMailService mailService) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.mailService = mailService;
    }

    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public void solicitar(String correo) {
        if (!mailService.isConfigured()) {
            log.warn("Recuperación solicitada, pero el servidor SMTP no está configurado.");
            throw new ReglaNegocioException("No se pudo enviar el código. El administrador debe configurar MAIL_HOST, MAIL_FROM y las credenciales SMTP del backend.");
        }

        String normalized = correo.trim().toLowerCase();
        Optional<Usuario> candidato = usuarios.findByCorreoIgnoreCase(normalized);
        // La respuesta HTTP no revela si la cuenta existe o está activa.
        if (candidato.isEmpty() || candidato.get().getEstado() != Estado.ACTIVO) return;

        Usuario user = candidato.get();
        List<PasswordResetToken> anteriores = tokens.findByUsuario_IdUsuarioAndUsadoFalse(user.getIdUsuario());
        anteriores.forEach(token -> token.setUsado(true));
        tokens.saveAll(anteriores);

        String code = String.format(Locale.ROOT, "%0" + CODE_DIGITS + "d", secureRandom.nextInt(100_000_000));
        PasswordResetToken entry = new PasswordResetToken();
        entry.setUsuario(user);
        entry.setTokenHash(hash(normalized + ":" + code));
        entry.setFechaExpiracion(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
        entry.setIntentos(0);
        tokens.save(entry);

        try {
            String texto = "Se solicitó cambiar la contraseña de su cuenta SMCV.\n\n"
                    + "Su código de verificación es: " + code + "\n\n"
                    + "El código vence en " + CODE_TTL_MINUTES + " minutos y permite hasta " + MAX_ATTEMPTS + " intentos.\n"
                    + "Si usted no solicitó este cambio, ignore este mensaje.";
            mailService.enviarTexto(List.of(user.getCorreo()), "Código para cambiar contraseña - SMCV", texto);
        } catch (Exception error) {
            entry.setUsado(true);
            tokens.save(entry);
            log.error("No se pudo enviar el código de recuperación por correo", error);
            throw new ReglaNegocioException("No se pudo enviar el código. Revisa la configuración SMTP del backend y vuelve a intentarlo.");
        }
    }

    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public void confirmar(String correo, String codigo, String passwordNueva, String confirmarPassword) {
        String normalized = correo == null ? "" : correo.trim().toLowerCase();
        if (passwordNueva == null || passwordNueva.length() < 8 || passwordNueva.length() > 72) {
            throw new ReglaNegocioException("La contraseña debe tener entre 8 y 72 caracteres");
        }
        if (!passwordNueva.equals(confirmarPassword)) {
            throw new ReglaNegocioException("Las contraseñas no coinciden");
        }
        if (codigo == null || !codigo.matches("\\d{" + CODE_DIGITS + "}")) {
            throw new ReglaNegocioException("El código debe contener " + CODE_DIGITS + " dígitos");
        }

        Usuario user = usuarios.findByCorreoIgnoreCase(normalized)
                .filter(usuario -> usuario.getEstado() == Estado.ACTIVO)
                .orElseThrow(() -> new ReglaNegocioException("El código no es válido o venció. Solicita uno nuevo."));
        PasswordResetToken entry = tokens.findFirstByUsuario_IdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(user.getIdUsuario())
                .orElseThrow(() -> new ReglaNegocioException("El código no es válido o venció. Solicita uno nuevo."));

        if (entry.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            entry.setUsado(true);
            tokens.save(entry);
            throw new ReglaNegocioException("El código venció. Solicita uno nuevo.");
        }
        if (entry.getIntentos() >= MAX_ATTEMPTS) {
            entry.setUsado(true);
            tokens.save(entry);
            throw new ReglaNegocioException("Se agotaron los intentos. Solicita un código nuevo.");
        }
        if (!entry.getTokenHash().equals(hash(normalized + ":" + codigo))) {
            int attempts = entry.getIntentos() + 1;
            entry.setIntentos(attempts);
            if (attempts >= MAX_ATTEMPTS) entry.setUsado(true);
            tokens.save(entry);
            throw new ReglaNegocioException(attempts >= MAX_ATTEMPTS
                    ? "Se agotaron los intentos. Solicita un código nuevo."
                    : "El código no coincide. Verifícalo e inténtalo nuevamente.");
        }

        user.setPasswordHash(encoder.encode(passwordNueva));
        usuarios.save(user);
        entry.setUsado(true);
        tokens.save(entry);
        List<PasswordResetToken> otros = tokens.findByUsuario_IdUsuarioAndUsadoFalse(user.getIdUsuario());
        otros.forEach(token -> token.setUsado(true));
        tokens.saveAll(otros);
        auditoria.registrarComo(user.getIdUsuario(), AccionAuditoria.CAMBIAR_PASSWORD, "USUARIO",
                user.getIdUsuario(), "Contraseña restablecida mediante código de verificación por correo", null, null);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo proteger el código de recuperación", error);
        }
    }
}
