package pe.smcv.ptar.reporte.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.convenio.entity.EntidadExterna;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

/**
 * Registro de que un reporte se entregó a una entidad externa (tabla reporte_entidad_externa).
 * El sistema NO envía el reporte: solo deja constancia de la entrega.
 */
@Entity
@Table(name = "reporte_entidad_externa")
public class ReporteEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Integer idEnvio;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "medio", nullable = false, length = 30)
    private MedioEnvio medio;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reporte", nullable = false)
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entidad", nullable = false)
    private EntidadExterna entidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    public Integer getIdEnvio() { return idEnvio; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public MedioEnvio getMedio() { return medio; }
    public void setMedio(MedioEnvio medio) { this.medio = medio; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Reporte getReporte() { return reporte; }
    public void setReporte(Reporte reporte) { this.reporte = reporte; }
    public EntidadExterna getEntidad() { return entidad; }
    public void setEntidad(EntidadExterna entidad) { this.entidad = entidad; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
