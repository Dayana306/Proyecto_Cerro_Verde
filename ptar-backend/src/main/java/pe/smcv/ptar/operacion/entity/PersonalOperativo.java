package pe.smcv.ptar.operacion.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_operativo")
public class PersonalOperativo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personal")
    private Integer idPersonal;
    @Column(name = "nombres", nullable = false, length = 90) private String nombres;
    @Column(name = "apellidos", nullable = false, length = 90) private String apellidos;
    @Column(name = "cargo", nullable = false, length = 100) private String cargo;
    @Column(name = "area", nullable = false, length = 100) private String area;
    @Column(name = "turno", nullable = false, length = 80) private String turno;
    @Column(name = "telefono", length = 25) private String telefono;
    @Column(name = "correo", length = 120) private String correo;
    @Column(name = "estado", nullable = false, length = 20) private String estado = "ACTIVO";
    @Column(name = "es_demo", nullable = false) private boolean demo;
    @Column(name = "fecha_creacion", nullable = false, updatable = false) private LocalDateTime fechaCreacion;

    @PrePersist void beforeInsert() { if (fechaCreacion == null) fechaCreacion = LocalDateTime.now(); }
    public Integer getIdPersonal() { return idPersonal; }
    public String getNombres() { return nombres; }
    public void setNombres(String v) { nombres = v; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String v) { apellidos = v; }
    public String getCargo() { return cargo; }
    public void setCargo(String v) { cargo = v; }
    public String getArea() { return area; }
    public void setArea(String v) { area = v; }
    public String getTurno() { return turno; }
    public void setTurno(String v) { turno = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { telefono = v; }
    public String getCorreo() { return correo; }
    public void setCorreo(String v) { correo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = v; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean v) { demo = v; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
