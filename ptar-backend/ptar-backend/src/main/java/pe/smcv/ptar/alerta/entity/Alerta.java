package pe.smcv.ptar.alerta.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.parametro.entity.Umbral;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Alerta generada automáticamente cuando una lectura excede su umbral (RF-05). */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_alerta", nullable = false, length = 30)
    private TipoAlerta tipoAlerta;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "nivel", nullable = false, length = 10)
    private NivelAlerta nivel;

    @Column(name = "valor_detectado", nullable = false, precision = 10, scale = 3)
    private BigDecimal valorDetectado;

    @Column(name = "fecha_deteccion", nullable = false, updatable = false)
    private LocalDateTime fechaDeteccion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAlerta estado = EstadoAlerta.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lectura", nullable = false)
    private LecturaParametro lectura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_umbral", nullable = false)
    private Umbral umbral;

    @PrePersist
    void alCrear() {
        if (fechaDeteccion == null) {
            fechaDeteccion = LocalDateTime.now();
        }
    }

    public Integer getIdAlerta() { return idAlerta; }
    public TipoAlerta getTipoAlerta() { return tipoAlerta; }
    public void setTipoAlerta(TipoAlerta tipoAlerta) { this.tipoAlerta = tipoAlerta; }
    public NivelAlerta getNivel() { return nivel; }
    public void setNivel(NivelAlerta nivel) { this.nivel = nivel; }
    public BigDecimal getValorDetectado() { return valorDetectado; }
    public void setValorDetectado(BigDecimal valorDetectado) { this.valorDetectado = valorDetectado; }
    public LocalDateTime getFechaDeteccion() { return fechaDeteccion; }
    public void setFechaDeteccion(LocalDateTime fechaDeteccion) { this.fechaDeteccion = fechaDeteccion; }
    public EstadoAlerta getEstado() { return estado; }
    public void setEstado(EstadoAlerta estado) { this.estado = estado; }
    public LecturaParametro getLectura() { return lectura; }
    public void setLectura(LecturaParametro lectura) { this.lectura = lectura; }
    public Umbral getUmbral() { return umbral; }
    public void setUmbral(Umbral umbral) { this.umbral = umbral; }
}
