package pe.smcv.ptar.ubicacion.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Estación / punto de monitoreo. */
@Entity
@Table(name = "punto_monitoreo")
public class PuntoMonitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_punto")
    private Integer idPunto;

    @Column(name = "nombre_punto", nullable = false, length = 100)
    private String nombrePunto;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_zona", nullable = false)
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_area", nullable = false)
    private Area area;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Integer getIdPunto() { return idPunto; }
    public String getNombrePunto() { return nombrePunto; }
    public void setNombrePunto(String nombrePunto) { this.nombrePunto = nombrePunto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
}
