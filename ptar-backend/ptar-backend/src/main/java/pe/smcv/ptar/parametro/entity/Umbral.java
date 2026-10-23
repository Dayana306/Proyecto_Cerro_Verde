package pe.smcv.ptar.parametro.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Umbral de alerta (RF-05/RF-06) por parámetro y, opcionalmente, por punto.
 * Los umbrales no se editan: un cambio desactiva el vigente y crea uno nuevo (trazabilidad).
 */
@Entity
@Table(name = "umbral")
public class Umbral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_umbral")
    private Integer idUmbral;

    @Column(name = "limite_min", precision = 10, scale = 3)
    private BigDecimal limiteMin;

    @Column(name = "limite_max", precision = 10, scale = 3)
    private BigDecimal limiteMax;

    @Column(name = "critico_min", precision = 10, scale = 3)
    private BigDecimal criticoMin;

    @Column(name = "critico_max", precision = 10, scale = 3)
    private BigDecimal criticoMax;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_parametro", nullable = false)
    private Parametro parametro;

    /** null = umbral general del parámetro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_punto")
    private PuntoMonitoreo punto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Integer getIdUmbral() { return idUmbral; }
    public BigDecimal getLimiteMin() { return limiteMin; }
    public void setLimiteMin(BigDecimal limiteMin) { this.limiteMin = limiteMin; }
    public BigDecimal getLimiteMax() { return limiteMax; }
    public void setLimiteMax(BigDecimal limiteMax) { this.limiteMax = limiteMax; }
    public BigDecimal getCriticoMin() { return criticoMin; }
    public void setCriticoMin(BigDecimal criticoMin) { this.criticoMin = criticoMin; }
    public BigDecimal getCriticoMax() { return criticoMax; }
    public void setCriticoMax(BigDecimal criticoMax) { this.criticoMax = criticoMax; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public Parametro getParametro() { return parametro; }
    public void setParametro(Parametro parametro) { this.parametro = parametro; }
    public PuntoMonitoreo getPunto() { return punto; }
    public void setPunto(PuntoMonitoreo punto) { this.punto = punto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
