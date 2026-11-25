package pe.smcv.ptar.convenio.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;

import java.time.LocalDateTime;

/** SEDAPAR, ANA, OEFA u otra institución con la que se intercambia información. */
@Entity
@Table(name = "entidad_externa")
public class EntidadExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entidad")
    private Integer idEntidad;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_entidad", nullable = false, length = 30)
    private TipoEntidad tipoEntidad;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Integer getIdEntidad() { return idEntidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoEntidad getTipoEntidad() { return tipoEntidad; }
    public void setTipoEntidad(TipoEntidad tipoEntidad) { this.tipoEntidad = tipoEntidad; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
