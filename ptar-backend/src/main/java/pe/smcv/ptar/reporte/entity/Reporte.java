package pe.smcv.ptar.reporte.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Reporte generado (RF-07). El archivo queda guardado para descargarlo de nuevo o auditarlo. */
@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private TipoReporte tipoReporte;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "formato", nullable = false, length = 10)
    private FormatoReporte formato;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "nombre_archivo", length = 150)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", length = 255)
    private String rutaArchivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReporte estado = EstadoReporte.GENERADO;

    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    /** null = todas las estaciones. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_punto")
    private PuntoMonitoreo punto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @PrePersist
    void alCrear() {
        if (fechaGeneracion == null) {
            fechaGeneracion = LocalDateTime.now();
        }
    }

    public Integer getIdReporte() { return idReporte; }
    public TipoReporte getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(TipoReporte tipoReporte) { this.tipoReporte = tipoReporte; }
    public FormatoReporte getFormato() { return formato; }
    public void setFormato(FormatoReporte formato) { this.formato = formato; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(LocalDate fechaDesde) { this.fechaDesde = fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(LocalDate fechaHasta) { this.fechaHasta = fechaHasta; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public PuntoMonitoreo getPunto() { return punto; }
    public void setPunto(PuntoMonitoreo punto) { this.punto = punto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
