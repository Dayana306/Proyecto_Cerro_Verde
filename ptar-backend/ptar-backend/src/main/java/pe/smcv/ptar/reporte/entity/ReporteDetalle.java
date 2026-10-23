package pe.smcv.ptar.reporte.entity;

import jakarta.persistence.*;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;

import java.math.BigDecimal;

/** Estadística consolidada de un parámetro en un punto dentro del periodo del reporte. */
@Entity
@Table(name = "reporte_detalle")
public class ReporteDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @Column(name = "cantidad_lecturas", nullable = false)
    private Integer cantidadLecturas;

    @Column(name = "valor_promedio", precision = 10, scale = 3)
    private BigDecimal valorPromedio;

    @Column(name = "valor_minimo", precision = 10, scale = 3)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 10, scale = 3)
    private BigDecimal valorMaximo;

    @Column(name = "cantidad_alertas", nullable = false)
    private Integer cantidadAlertas = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reporte", nullable = false)
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_parametro", nullable = false)
    private Parametro parametro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_punto", nullable = false)
    private PuntoMonitoreo punto;

    public Integer getIdDetalle() { return idDetalle; }
    public Integer getCantidadLecturas() { return cantidadLecturas; }
    public void setCantidadLecturas(Integer cantidadLecturas) { this.cantidadLecturas = cantidadLecturas; }
    public BigDecimal getValorPromedio() { return valorPromedio; }
    public void setValorPromedio(BigDecimal valorPromedio) { this.valorPromedio = valorPromedio; }
    public BigDecimal getValorMinimo() { return valorMinimo; }
    public void setValorMinimo(BigDecimal valorMinimo) { this.valorMinimo = valorMinimo; }
    public BigDecimal getValorMaximo() { return valorMaximo; }
    public void setValorMaximo(BigDecimal valorMaximo) { this.valorMaximo = valorMaximo; }
    public Integer getCantidadAlertas() { return cantidadAlertas; }
    public void setCantidadAlertas(Integer cantidadAlertas) { this.cantidadAlertas = cantidadAlertas; }
    public Reporte getReporte() { return reporte; }
    public void setReporte(Reporte reporte) { this.reporte = reporte; }
    public Parametro getParametro() { return parametro; }
    public void setParametro(Parametro parametro) { this.parametro = parametro; }
    public PuntoMonitoreo getPunto() { return punto; }
    public void setPunto(PuntoMonitoreo punto) { this.punto = punto; }
}
