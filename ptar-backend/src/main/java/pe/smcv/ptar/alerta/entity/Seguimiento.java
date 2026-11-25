package pe.smcv.ptar.alerta.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

/** Acción registrada sobre una alerta (comentario + cambio de estado). */
@Entity
@Table(name = "seguimiento")
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    private Integer idSeguimiento;

    @Column(name = "comentario", nullable = false, length = 500)
    private String comentario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoAlerta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoAlerta estadoNuevo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Integer getIdSeguimiento() { return idSeguimiento; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public EstadoAlerta getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoAlerta estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoAlerta getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoAlerta estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public LocalDateTime getFecha() { return fecha; }
    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
