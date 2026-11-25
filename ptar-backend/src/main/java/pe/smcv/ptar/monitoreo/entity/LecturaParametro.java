package pe.smcv.ptar.monitoreo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.entity.Parametro;

import java.math.BigDecimal;

/** Valor de un parámetro dentro de un monitoreo. Entidad central del modelo. */
@Entity
@Table(name = "lectura_parametro")
public class LecturaParametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura")
    private Long idLectura;

    @Column(name = "valor", nullable = false, precision = 10, scale = 3)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_monitoreo", nullable = false)
    private Monitoreo monitoreo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_parametro", nullable = false)
    private Parametro parametro;

    public Long getIdLectura() { return idLectura; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Monitoreo getMonitoreo() { return monitoreo; }
    public void setMonitoreo(Monitoreo monitoreo) { this.monitoreo = monitoreo; }
    public Parametro getParametro() { return parametro; }
    public void setParametro(Parametro parametro) { this.parametro = parametro; }
}
