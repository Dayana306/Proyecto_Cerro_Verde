package pe.smcv.ptar.parametro.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;

import java.math.BigDecimal;

/** Parámetro medido (pH, caudal, turbidez, temperatura...). */
@Entity
@Table(name = "parametro")
public class Parametro {

    /** Código que el backend usa para el cálculo del convenio hídrico. */
    public static final String CODIGO_CAUDAL = "CAUDAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Integer idParametro;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nombre_parametro", nullable = false, length = 60)
    private String nombreParametro;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "valor_min_valido", nullable = false, precision = 10, scale = 3)
    private BigDecimal valorMinValido;

    @Column(name = "valor_max_valido", nullable = false, precision = 10, scale = 3)
    private BigDecimal valorMaxValido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    /** RF-03: ¿el valor es físicamente posible? */
    public boolean esValorValido(BigDecimal valor) {
        return valor.compareTo(valorMinValido) >= 0 && valor.compareTo(valorMaxValido) <= 0;
    }

    public Integer getIdParametro() { return idParametro; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombreParametro() { return nombreParametro; }
    public void setNombreParametro(String nombreParametro) { this.nombreParametro = nombreParametro; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
    public BigDecimal getValorMinValido() { return valorMinValido; }
    public void setValorMinValido(BigDecimal valorMinValido) { this.valorMinValido = valorMinValido; }
    public BigDecimal getValorMaxValido() { return valorMaxValido; }
    public void setValorMaxValido(BigDecimal valorMaxValido) { this.valorMaxValido = valorMaxValido; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
