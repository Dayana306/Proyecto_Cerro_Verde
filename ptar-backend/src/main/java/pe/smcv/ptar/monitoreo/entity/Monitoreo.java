package pe.smcv.ptar.monitoreo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Un muestreo en un punto y momento dados; agrupa las lecturas de varios parámetros. */
@Entity
@Table(name = "monitoreo")
public class Monitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_monitoreo")
    private Integer idMonitoreo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_muestreo", nullable = false, length = 30)
    private TipoMuestreo tipoMuestreo;

    @Column(name = "hora_muestreo", nullable = false)
    private LocalDateTime horaMuestreo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "observacion", length = 255)
    private String observacion;

    /** INACTIVO = anulado. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_punto", nullable = false)
    private PuntoMonitoreo punto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "monitoreo", cascade = CascadeType.ALL)
    @OrderBy("idLectura")
    private List<LecturaParametro> lecturas = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public void agregarLectura(LecturaParametro lectura) {
        lectura.setMonitoreo(this);
        lecturas.add(lectura);
    }

    public Integer getIdMonitoreo() { return idMonitoreo; }
    public TipoMuestreo getTipoMuestreo() { return tipoMuestreo; }
    public void setTipoMuestreo(TipoMuestreo tipoMuestreo) { this.tipoMuestreo = tipoMuestreo; }
    public LocalDateTime getHoraMuestreo() { return horaMuestreo; }
    public void setHoraMuestreo(LocalDateTime horaMuestreo) { this.horaMuestreo = horaMuestreo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public PuntoMonitoreo getPunto() { return punto; }
    public void setPunto(PuntoMonitoreo punto) { this.punto = punto; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public List<LecturaParametro> getLecturas() { return lecturas; }
}
