package pe.smcv.ptar.auditoria.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

/** Bitácora de auditoría (solo inserción). */
@Entity
@Table(name = "historial")
public class Historial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "accion", nullable = false, length = 30, updatable = false)
    private AccionAuditoria accion;

    @Column(name = "entidad", nullable = false, length = 40, updatable = false)
    private String entidad;

    @Column(name = "id_entidad", updatable = false)
    private Long idEntidad;

    @Column(name = "descripcion", length = 255, updatable = false)
    private String descripcion;

    @Column(name = "datos_anteriores", columnDefinition = "TEXT", updatable = false)
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "TEXT", updatable = false)
    private String datosNuevos;

    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", updatable = false)
    private Usuario usuario;

    @PrePersist
    void alCrear() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public Long getIdHistorial() { return idHistorial; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public AccionAuditoria getAccion() { return accion; }
    public void setAccion(AccionAuditoria accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDatosAnteriores() { return datosAnteriores; }
    public void setDatosAnteriores(String datosAnteriores) { this.datosAnteriores = datosAnteriores; }
    public String getDatosNuevos() { return datosNuevos; }
    public void setDatosNuevos(String datosNuevos) { this.datosNuevos = datosNuevos; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
