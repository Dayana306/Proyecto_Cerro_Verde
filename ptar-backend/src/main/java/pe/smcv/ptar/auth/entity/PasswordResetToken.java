package pe.smcv.ptar.auth.entity;

import jakarta.persistence.*;
import pe.smcv.ptar.usuario.entity.Usuario;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="password_reset_token")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_token") private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="id_usuario", nullable=false) private Usuario usuario;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name="fecha_expiracion", nullable=false) private LocalDateTime fechaExpiracion;
    @Column(name="usado", nullable=false) private boolean usado=false;
    @Column(name="fecha_creacion", nullable=false, updatable=false) private LocalDateTime fechaCreacion;
    @PrePersist void onCreate(){ if(fechaCreacion==null) fechaCreacion=LocalDateTime.now(); }
    public Long getId(){return id;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario u){usuario=u;}
    public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
    public LocalDateTime getFechaExpiracion(){return fechaExpiracion;} public void setFechaExpiracion(LocalDateTime v){fechaExpiracion=v;}
    public boolean isUsado(){return usado;} public void setUsado(boolean v){usado=v;}
}
