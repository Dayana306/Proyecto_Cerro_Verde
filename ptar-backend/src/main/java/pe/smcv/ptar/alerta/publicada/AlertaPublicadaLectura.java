package pe.smcv.ptar.alerta.publicada;

import jakarta.persistence.*;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta_publicada_lectura", uniqueConstraints =
        @UniqueConstraint(name = "uq_alerta_publicada_usuario", columnNames = {"id_alerta_publicada", "id_usuario"}))
public class AlertaPublicadaLectura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura")
    private Integer idLectura;

    @Column(name = "fecha_lectura", nullable = false, updatable = false)
    private LocalDateTime fechaLectura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alerta_publicada", nullable = false)
    private AlertaPublicada alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @PrePersist
    void prePersist() { if (fechaLectura == null) fechaLectura = LocalDateTime.now(); }

    public Integer getIdLectura() { return idLectura; }
    public LocalDateTime getFechaLectura() { return fechaLectura; }
    public AlertaPublicada getAlerta() { return alerta; }
    public void setAlerta(AlertaPublicada alerta) { this.alerta = alerta; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
