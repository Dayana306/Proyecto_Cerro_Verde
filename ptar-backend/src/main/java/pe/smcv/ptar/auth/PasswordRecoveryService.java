package pe.smcv.ptar.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.Base64;

@Service
public class PasswordRecoveryService {
    private static final Logger log=LoggerFactory.getLogger(PasswordRecoveryService.class);
    private final UsuarioRepository usuarios; private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder; private final AuditoriaService auditoria;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    @Value("${spring.mail.host:}") private String mailHost;
    @Value("${spring.mail.username:}") private String mailUsername;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;
    public PasswordRecoveryService(UsuarioRepository usuarios, PasswordResetTokenRepository tokens, PasswordEncoder encoder, AuditoriaService auditoria, ObjectProvider<JavaMailSender> mailSenderProvider){
        this.usuarios=usuarios;this.tokens=tokens;this.encoder=encoder;this.auditoria=auditoria;this.mailSenderProvider=mailSenderProvider;
    }
    @Transactional
    public void solicitar(String correo){
        String normalized=correo.trim().toLowerCase();
        Optional<Usuario> candidato=usuarios.findByCorreoIgnoreCase(normalized);
        if(candidato.isEmpty() || candidato.get().getEstado()!=Estado.ACTIVO) return;
        JavaMailSender sender=mailSenderProvider.getIfAvailable();
        if(sender==null || mailHost==null || mailHost.isBlank()){
            log.warn("Recuperación solicitada para una cuenta existente, pero SMTP no está configurado. Configure SPRING_MAIL_HOST/USERNAME/PASSWORD; por seguridad no se expone el token por la API.");
            return;
        }
        Usuario user=candidato.get();
        List<PasswordResetToken> anteriores=tokens.findByUsuario_IdUsuarioAndUsadoFalse(user.getIdUsuario());
        anteriores.forEach(t->t.setUsado(true)); tokens.saveAll(anteriores);
        byte[] random=new byte[48]; new SecureRandom().nextBytes(random);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        PasswordResetToken entry=new PasswordResetToken(); entry.setUsuario(user); entry.setTokenHash(hash(token)); entry.setFechaExpiracion(LocalDateTime.now().plusMinutes(30)); tokens.save(entry);
        String link=frontendUrl.replaceAll("/$","")+"/?resetToken="+token;
        try { SimpleMailMessage message=new SimpleMailMessage(); message.setTo(user.getCorreo()); message.setSubject("Restablecer contraseña - SMCV"); message.setText("Se solicitó restablecer la contraseña de su cuenta SMCV.\n\nAbra este enlace en los próximos 30 minutos:\n"+link+"\n\nSi no hizo esta solicitud, ignore este mensaje.\nPor seguridad, el enlace solo puede utilizarse una vez."); if(mailUsername!=null&&!mailUsername.isBlank()) message.setFrom(mailUsername); sender.send(message); }
        catch(Exception e){ entry.setUsado(true); tokens.save(entry); log.error("No se pudo enviar un correo de recuperación; se mantiene respuesta pública genérica",e); }
    }
    @Transactional
    public void confirmar(String rawToken,String passwordNueva){
        if(passwordNueva==null || passwordNueva.length()<8 || passwordNueva.length()>72) throw new ReglaNegocioException("La contraseña debe tener entre 8 y 72 caracteres");
        PasswordResetToken entry=tokens.findByTokenHashAndUsadoFalse(hash(rawToken)).orElseThrow(()->new ReglaNegocioException("El enlace de recuperación no es válido o ya fue utilizado"));
        if(entry.getFechaExpiracion().isBefore(LocalDateTime.now())){entry.setUsado(true);tokens.save(entry);throw new ReglaNegocioException("El enlace de recuperación venció. Solicita uno nuevo.");}
        Usuario user=entry.getUsuario(); user.setPasswordHash(encoder.encode(passwordNueva)); usuarios.save(user); entry.setUsado(true); tokens.save(entry);
        List<PasswordResetToken> otros=tokens.findByUsuario_IdUsuarioAndUsadoFalse(user.getIdUsuario()); otros.forEach(t->t.setUsado(true));tokens.saveAll(otros);
        auditoria.registrarComo(user.getIdUsuario(),AccionAuditoria.CAMBIAR_PASSWORD,"USUARIO",user.getIdUsuario(),"Contraseña restablecida mediante enlace de correo",null,null);
    }
    private String hash(String token){try {MessageDigest md=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No se pudo generar hash de token",e);}}
}
