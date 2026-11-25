package pe.smcv.ptar.operacion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimiento_programado")
public class MantenimientoProgramado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_mantenimiento") private Long idMantenimiento;
    @Column(name = "id_equipo", nullable = false) private Integer idEquipo;
    @Column(name = "id_personal_asignado", nullable = false) private Integer idPersonalAsignado;
    @Column(name = "fecha_hora_programada", nullable = false) private LocalDateTime fechaHoraProgramada;
    @Column(name = "tipo", nullable = false, length = 60) private String tipo;
    @Column(name = "descripcion", nullable = false, length = 1500) private String descripcion;
    @Column(name = "estado", nullable = false, length = 30) private String estado = "PROGRAMADO";
    @Column(name = "resultado", length = 2000) private String resultado;
    @Column(name = "id_usuario_creador", nullable = false) private Integer idUsuarioCreador;
    @Column(name = "fecha_creacion", nullable = false, updatable = false) private LocalDateTime fechaCreacion;
    @PrePersist void beforeInsert(){if(fechaCreacion==null)fechaCreacion=LocalDateTime.now();}
    public Long getIdMantenimiento(){return idMantenimiento;} public Integer getIdEquipo(){return idEquipo;} public void setIdEquipo(Integer v){idEquipo=v;}
    public Integer getIdPersonalAsignado(){return idPersonalAsignado;} public void setIdPersonalAsignado(Integer v){idPersonalAsignado=v;}
    public LocalDateTime getFechaHoraProgramada(){return fechaHoraProgramada;} public void setFechaHoraProgramada(LocalDateTime v){fechaHoraProgramada=v;}
    public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;} public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;} public String getResultado(){return resultado;} public void setResultado(String v){resultado=v;}
    public Integer getIdUsuarioCreador(){return idUsuarioCreador;} public void setIdUsuarioCreador(Integer v){idUsuarioCreador=v;} public LocalDateTime getFechaCreacion(){return fechaCreacion;}
}
