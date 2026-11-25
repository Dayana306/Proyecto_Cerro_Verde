package pe.smcv.ptar.alerta.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

/** Aviso de una alerta a un usuario (campana del panel). */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotificacion canal = CanalNotificacion.PANEL;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    /** null = no leída. */
    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @PrePersist
    void alCrear() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
    }

    public Integer getIdNotificacion() { return idNotificacion; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public CanalNotificacion getCanal() { return canal; }
    public void setCanal(CanalNotificacion canal) { this.canal = canal; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public LocalDateTime getFechaLectura() { return fechaLectura; }
    public void setFechaLectura(LocalDateTime fechaLectura) { this.fechaLectura = fechaLectura; }
    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
