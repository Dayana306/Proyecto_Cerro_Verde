package pe.smcv.ptar.operacion.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipo_monitoreo")
public class EquipoMonitoreo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo") private Integer idEquipo;
    @Column(name = "codigo", nullable = false, unique = true, length = 35) private String codigo;
    @Column(name = "nombre", nullable = false, length = 130) private String nombre;
    @Column(name = "tipo_equipo", nullable = false, length = 80) private String tipoEquipo;
    @Column(name = "fabricante", length = 100) private String fabricante;
    @Column(name = "modelo", length = 100) private String modelo;
    @Column(name = "modo_lectura", nullable = false, length = 25) private String modoLectura = "MANUAL";
    @Column(name = "estado", nullable = false, length = 25) private String estado = "ACTIVO";
    @Column(name = "estado_conexion", nullable = false, length = 25) private String estadoConexion = "NO_APLICA";
    @Column(name = "id_punto") private Integer idPunto;
    @Column(name = "id_personal_responsable") private Integer idPersonalResponsable;
    @Column(name = "ultima_lectura_en") private LocalDateTime ultimaLecturaEn;
    @Column(name = "ultima_revision") private LocalDate ultimaRevision;
    @Column(name = "proxima_revision") private LocalDate proximaRevision;
    @Column(name = "observaciones", length = 1000) private String observaciones;
    @Column(name = "es_demo", nullable = false) private boolean demo;
    @Column(name = "fecha_creacion", nullable = false, updatable = false) private LocalDateTime fechaCreacion;
    @PrePersist void beforeInsert() { if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(); }
    public Integer getIdEquipo(){return idEquipo;} public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;} public String getTipoEquipo(){return tipoEquipo;} public void setTipoEquipo(String v){tipoEquipo=v;}
    public String getFabricante(){return fabricante;} public void setFabricante(String v){fabricante=v;} public String getModelo(){return modelo;} public void setModelo(String v){modelo=v;}
    public String getModoLectura(){return modoLectura;} public void setModoLectura(String v){modoLectura=v;} public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getEstadoConexion(){return estadoConexion;} public void setEstadoConexion(String v){estadoConexion=v;} public Integer getIdPunto(){return idPunto;} public void setIdPunto(Integer v){idPunto=v;}
    public Integer getIdPersonalResponsable(){return idPersonalResponsable;} public void setIdPersonalResponsable(Integer v){idPersonalResponsable=v;}
    public LocalDateTime getUltimaLecturaEn(){return ultimaLecturaEn;} public void setUltimaLecturaEn(LocalDateTime v){ultimaLecturaEn=v;}
    public LocalDate getUltimaRevision(){return ultimaRevision;} public void setUltimaRevision(LocalDate v){ultimaRevision=v;} public LocalDate getProximaRevision(){return proximaRevision;} public void setProximaRevision(LocalDate v){proximaRevision=v;}
    public String getObservaciones(){return observaciones;} public void setObservaciones(String v){observaciones=v;} public boolean isDemo(){return demo;} public void setDemo(boolean v){demo=v;} public LocalDateTime getFechaCreacion(){return fechaCreacion;}
}
