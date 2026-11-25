package pe.smcv.ptar.ubicacion.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;

import java.time.LocalDateTime;

/** Imagen base del visor 2D. Las zonas (GeoJSON) y puntos (lat/lon) se dibujan encima. */
@Entity
@Table(name = "mapa_2d")
public class Mapa2d {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mapa")
    private Integer idMapa;

    @Column(name = "nombre_mapa", nullable = false, length = 100)
    private String nombreMapa;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "imagen_ruta", nullable = false, length = 255)
    private String imagenRuta;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void alGuardar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Integer getIdMapa() { return idMapa; }
    public String getNombreMapa() { return nombreMapa; }
    public void setNombreMapa(String nombreMapa) { this.nombreMapa = nombreMapa; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagenRuta() { return imagenRuta; }
    public void setImagenRuta(String imagenRuta) { this.imagenRuta = imagenRuta; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
