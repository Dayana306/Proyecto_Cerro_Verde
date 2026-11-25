package pe.smcv.ptar.notificacion.correo;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/** Envío SMTP con Jakarta Mail. Admite texto, HTML y archivos adjuntos MIME. */
@Service
public class JakartaMailService {
    @Value("${app.mail.host:}") private String host;
    @Value("${app.mail.port:587}") private int port;
    @Value("${app.mail.username:}") private String username;
    @Value("${app.mail.password:}") private String password;
    @Value("${app.mail.from:}") private String from;
    @Value("${app.mail.auth:true}") private boolean auth;
    @Value("${app.mail.starttls:true}") private boolean starttls;
    @Value("${app.mail.ssl:false}") private boolean ssl;

    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && port > 0 && port <= 65535
                && fromAddress() != null && !fromAddress().isBlank()
                && !(starttls && ssl)
                && (!auth || (username != null && !username.isBlank() && password != null && !password.isBlank()));
    }

    public MailStatus status() {
        boolean configured = isConfigured();
        String message = configured
                ? "Servidor SMTP configurado. Puedes realizar un envío de prueba."
                : "Falta configurar MAIL_HOST, MAIL_FROM y, cuando se requiere autenticación, MAIL_USERNAME y MAIL_PASSWORD.";
        return new MailStatus(configured, safe(host), port, auth, starttls, ssl, message);
    }

    public void enviarTexto(Collection<String> destinatarios, String asunto, String texto)
            throws jakarta.mail.MessagingException, IOException {
        enviar(destinatarios, asunto, texto, false, List.of());
    }

    public void enviar(Collection<String> destinatarios, String asunto, String contenido, boolean html,
                       Collection<MultipartFile> adjuntos)
            throws jakarta.mail.MessagingException, IOException {
        if (!isConfigured()) {
            throw new IllegalStateException(status().mensaje());
        }
        if (destinatarios == null || destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un destinatario.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host.trim());
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.starttls.required", String.valueOf(starttls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");
        props.put("mail.mime.charset", "UTF-8");

        Session session = Session.getInstance(props);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress(), "PTAR La Enlozada", StandardCharsets.UTF_8.name()));
        InternetAddress[] to = destinatarios.stream()
                .map(String::trim)
                .map(address -> {
                    try {
                        InternetAddress parsed = new InternetAddress(address, true);
                        parsed.validate();
                        return parsed;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Dirección de correo inválida: " + address);
                    }
                })
                .toArray(InternetAddress[]::new);
        // Oculta la lista de destinatarios entre sí: los correos reales viajan como BCC.
        // El encabezado TO muestra el remitente del sistema y no expone las demás direcciones.
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(fromAddress(), "PTAR La Enlozada", StandardCharsets.UTF_8.name()));
        message.setRecipients(Message.RecipientType.BCC, to);
        message.setSubject(asunto, StandardCharsets.UTF_8.name());

        Multipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart = new MimeBodyPart();
        if (html) {
            bodyPart.setContent(contenido, "text/html; charset=UTF-8");
        } else {
            bodyPart.setText(contenido, StandardCharsets.UTF_8.name());
        }
        multipart.addBodyPart(bodyPart);

        if (adjuntos != null) {
            for (MultipartFile file : adjuntos) {
                if (file == null || file.isEmpty()) continue;
                String fileName = safeFileName(file.getOriginalFilename());
                String contentType = file.getContentType();
                if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(file.getBytes(), contentType)));
                attachment.setFileName(MimeUtility.encodeText(fileName, StandardCharsets.UTF_8.name(), "B"));
                attachment.setDisposition(Part.ATTACHMENT);
                multipart.addBodyPart(attachment);
            }
        }
        message.setContent(multipart);
        message.saveChanges();

        try (Transport transport = session.getTransport("smtp")) {
            if (auth) transport.connect(host.trim(), port, username.trim(), password);
            else transport.connect(host.trim(), port, null, null);
            transport.sendMessage(message, to);
        }
    }

    private String fromAddress() {
        return from == null || from.isBlank() ? username : from;
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static String safeFileName(String value) {
        if (value == null || value.isBlank()) return "adjunto";
        String clean = value.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\\"]", "_").trim();
        return clean.isBlank() ? "adjunto" : clean;
    }

    public record MailStatus(boolean configurado, String servidor, int puerto,
                             boolean autenticacion, boolean starttls, boolean ssl, String mensaje) {}
}
