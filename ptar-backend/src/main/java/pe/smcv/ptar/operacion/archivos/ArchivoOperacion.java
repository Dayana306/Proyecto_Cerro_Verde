package pe.smcv.ptar.operacion.archivos;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "archivo_operacion")
public class ArchivoOperacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_archivo") private Long idArchivo;
    @Column(name = "id_incidencia") private Long idIncidencia;
    @Column(name = "id_usuario_subio", nullable = false) private Integer idUsuarioSubio;
    @Column(name = "clase", nullable = false, length = 30) private String clase;
    @Column(name = "titulo", nullable = false, length = 160) private String titulo;
    @Column(name = "nombre_original", nullable = false, length = 255) private String nombreOriginal;
    @Column(name = "ruta_archivo", nullable = false, length = 255) private String rutaArchivo;
    @Column(name = "tipo_contenido", length = 120) private String tipoContenido;
    @Column(name = "tamano_bytes", nullable = false) private Long tamanoBytes;
    @Column(name = "descripcion", length = 1000) private String descripcion;
    @Column(name = "publicado", nullable = false) private boolean publicado = false;
    @Column(name = "estado", nullable = false, length = 20) private String estado = "ACTIVO";
    @Column(name = "fecha_subida", nullable = false, updatable = false) private LocalDateTime fechaSubida;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;
    @Column(name = "id_usuario_actualiza") private Integer idUsuarioActualiza;

    @PrePersist void crear() { LocalDateTime now=LocalDateTime.now(); if(fechaSubida==null)fechaSubida=now; fechaActualizacion=now; }
    @PreUpdate void actualizarFecha() { fechaActualizacion=LocalDateTime.now(); }
    public Long getIdArchivo(){return idArchivo;} public Long getIdIncidencia(){return idIncidencia;} public void setIdIncidencia(Long v){idIncidencia=v;}
    public Integer getIdUsuarioSubio(){return idUsuarioSubio;} public void setIdUsuarioSubio(Integer v){idUsuarioSubio=v;}
    public String getClase(){return clase;} public void setClase(String v){clase=v;} public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getNombreOriginal(){return nombreOriginal;} public void setNombreOriginal(String v){nombreOriginal=v;} public String getRutaArchivo(){return rutaArchivo;} public void setRutaArchivo(String v){rutaArchivo=v;}
    public String getTipoContenido(){return tipoContenido;} public void setTipoContenido(String v){tipoContenido=v;} public Long getTamanoBytes(){return tamanoBytes;} public void setTamanoBytes(Long v){tamanoBytes=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public boolean isPublicado(){return publicado;} public void setPublicado(boolean v){publicado=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;} public LocalDateTime getFechaSubida(){return fechaSubida;} public LocalDateTime getFechaActualizacion(){return fechaActualizacion;}
    public Integer getIdUsuarioActualiza(){return idUsuarioActualiza;} public void setIdUsuarioActualiza(Integer v){idUsuarioActualiza=v;}
}
