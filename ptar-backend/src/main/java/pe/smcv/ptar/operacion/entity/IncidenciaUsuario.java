package pe.smcv.ptar.operacion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia_usuario")
public class IncidenciaUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_incidencia") private Long idIncidencia;
    @Column(name = "categoria", nullable = false, length = 50) private String categoria;
    @Column(name = "titulo", nullable = false, length = 140) private String titulo;
    @Column(name = "zona", nullable = false, length = 180) private String zona;
    @Column(name = "descripcion", nullable = false, length = 4000) private String descripcion;
    @Column(name = "estado", nullable = false, length = 30) private String estado = "PENDIENTE";
    @Column(name = "prioridad", nullable = false, length = 20) private String prioridad = "MEDIA";
    @Column(name = "fecha_reporte", nullable = false, updatable = false) private LocalDateTime fechaReporte;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;
    @Column(name = "id_usuario_reporta", nullable = false) private Integer idUsuarioReporta;
    @Column(name = "nombre_reportante", nullable = false, length = 170) private String nombreReportante;
    @Column(name = "id_equipo") private Integer idEquipo;
    @Column(name = "id_personal_asignado") private Integer idPersonalAsignado;
    @Column(name = "fecha_hora_revision") private LocalDateTime fechaHoraRevision;
    @Column(name = "resultado_resolucion", length = 2000) private String resultadoResolucion;

    @PrePersist void beforeInsert(){ LocalDateTime now=LocalDateTime.now(); if(fechaReporte==null)fechaReporte=now; fechaActualizacion=now; }
    @PreUpdate void beforeUpdate(){fechaActualizacion=LocalDateTime.now();}
    public Long getIdIncidencia(){return idIncidencia;} public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;} public String getZona(){return zona;} public void setZona(String v){zona=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getPrioridad(){return prioridad;} public void setPrioridad(String v){prioridad=v;} public LocalDateTime getFechaReporte(){return fechaReporte;}
    public LocalDateTime getFechaActualizacion(){return fechaActualizacion;} public Integer getIdUsuarioReporta(){return idUsuarioReporta;} public void setIdUsuarioReporta(Integer v){idUsuarioReporta=v;}
    public String getNombreReportante(){return nombreReportante;} public void setNombreReportante(String v){nombreReportante=v;} public Integer getIdEquipo(){return idEquipo;} public void setIdEquipo(Integer v){idEquipo=v;}
    public Integer getIdPersonalAsignado(){return idPersonalAsignado;} public void setIdPersonalAsignado(Integer v){idPersonalAsignado=v;}
    public LocalDateTime getFechaHoraRevision(){return fechaHoraRevision;} public void setFechaHoraRevision(LocalDateTime v){fechaHoraRevision=v;}
    public String getResultadoResolucion(){return resultadoResolucion;} public void setResultadoResolucion(String v){resultadoResolucion=v;}
}
