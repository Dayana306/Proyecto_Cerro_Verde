package pe.smcv.ptar.alerta.publicada;

import jakarta.persistence.*;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta_publicada")
public class AlertaPublicada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta_publicada")
    private Integer idAlertaPublicada;

    @Column(name = "titulo", nullable = false, length = 120)
    private String titulo;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 10)
    private NivelAlerta nivel = NivelAlerta.MEDIA;

    @Column(name = "area", length = 120)
    private String area;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private Usuario creador;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    public Integer getIdAlertaPublicada() { return idAlertaPublicada; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public NivelAlerta getNivel() { return nivel; }
    public void setNivel(NivelAlerta nivel) { this.nivel = nivel; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public Usuario getCreador() { return creador; }
    public void setCreador(Usuario creador) { this.creador = creador; }
}
