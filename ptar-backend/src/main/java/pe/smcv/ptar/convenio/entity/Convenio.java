package pe.smcv.ptar.convenio.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Convenio hídrico (ej. con SEDAPAR) con meta de caudal (CU10). */
@Entity
@Table(name = "convenio")
public class Convenio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convenio")
    private Integer idConvenio;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "caudal_meta_m3s", nullable = false, precision = 6, scale = 3)
    private BigDecimal caudalMetaM3s;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_meta", nullable = false, length = 30)
    private TipoMeta tipoMeta = TipoMeta.PROMEDIO_ANUAL;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entidad", nullable = false)
    private EntidadExterna entidad;

    /** Punto donde se mide el caudal del convenio; null = promedio de todos los puntos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_punto")
    private PuntoMonitoreo punto;

    public Integer getIdConvenio() { return idConvenio; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public BigDecimal getCaudalMetaM3s() { return caudalMetaM3s; }
    public void setCaudalMetaM3s(BigDecimal caudalMetaM3s) { this.caudalMetaM3s = caudalMetaM3s; }
    public TipoMeta getTipoMeta() { return tipoMeta; }
    public void setTipoMeta(TipoMeta tipoMeta) { this.tipoMeta = tipoMeta; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public EntidadExterna getEntidad() { return entidad; }
    public void setEntidad(EntidadExterna entidad) { this.entidad = entidad; }
    public PuntoMonitoreo getPunto() { return punto; }
    public void setPunto(PuntoMonitoreo punto) { this.punto = punto; }
}
