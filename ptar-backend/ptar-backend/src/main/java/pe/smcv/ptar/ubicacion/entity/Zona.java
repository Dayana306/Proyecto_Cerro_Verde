package pe.smcv.ptar.ubicacion.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;

import java.time.LocalDateTime;

@Entity
@Table(name = "zona")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zona")
    private Integer idZona;

    @Column(name = "nombre_zona", nullable = false, length = 100)
    private String nombreZona;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_zona", nullable = false, length = 30)
    private TipoZona tipoZona = TipoZona.AREA_INFLUENCIA;

    /** GeoJSON en texto. Se valida en ZonaService. */
    @Column(name = "poligono_geojson", columnDefinition = "MEDIUMTEXT")
    private String poligonoGeojson;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_region", nullable = false)
    private Region region;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Integer getIdZona() { return idZona; }
    public String getNombreZona() { return nombreZona; }
    public void setNombreZona(String nombreZona) { this.nombreZona = nombreZona; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoZona getTipoZona() { return tipoZona; }
    public void setTipoZona(TipoZona tipoZona) { this.tipoZona = tipoZona; }
    public String getPoligonoGeojson() { return poligonoGeojson; }
    public void setPoligonoGeojson(String poligonoGeojson) { this.poligonoGeojson = poligonoGeojson; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
}
