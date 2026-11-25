package pe.smcv.ptar.notificacion.correo;

import jakarta.persistence.*;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

/** Bitácora persistente de los correos enviados desde el módulo Notificaciones. */
@Entity
@Table(name = "correo_notificacion")
public class CorreoNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correo")
    private Long idCorreo;

    @Column(name = "destinatarios", nullable = false, columnDefinition = "TEXT")
    private String destinatarios;

    @Column(name = "asunto", nullable = false, length = 180)
    private String asunto;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "es_html", nullable = false)
    private boolean html;

    @Column(name = "nombres_adjuntos", columnDefinition = "TEXT")
    private String nombresAdjuntos;

    @Column(name = "estado", nullable = false, length = 16)
    private String estado;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "error_resumen", length = 500)
    private String errorResumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @PrePersist
    void alCrear() { if (fechaEnvio == null) fechaEnvio = LocalDateTime.now(); }

    public Long getIdCorreo() { return idCorreo; }
    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(String value) { destinatarios = value; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String value) { asunto = value; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String value) { mensaje = value; }
    public boolean isHtml() { return html; }
    public void setHtml(boolean value) { html = value; }
    public String getNombresAdjuntos() { return nombresAdjuntos; }
    public void setNombresAdjuntos(String value) { nombresAdjuntos = value; }
    public String getEstado() { return estado; }
    public void setEstado(String value) { estado = value; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public String getErrorResumen() { return errorResumen; }
    public void setErrorResumen(String value) { errorResumen = value; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario value) { usuario = value; }
}
