package pe.smcv.ptar.operacion.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.operacion.entity.EquipoMonitoreo;
import pe.smcv.ptar.operacion.entity.IncidenciaUsuario;
import pe.smcv.ptar.operacion.entity.MantenimientoProgramado;
import pe.smcv.ptar.operacion.entity.PersonalOperativo;
import pe.smcv.ptar.operacion.repository.EquipoMonitoreoRepository;
import pe.smcv.ptar.operacion.repository.IncidenciaUsuarioRepository;
import pe.smcv.ptar.operacion.repository.MantenimientoProgramadoRepository;
import pe.smcv.ptar.operacion.repository.PersonalOperativoRepository;
import pe.smcv.ptar.security.UsuarioPrincipal;
import pe.smcv.ptar.ubicacion.repository.PuntoMonitoreoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Operación del Centro de Alertas: incidencias ciudadanas, equipos, personal y mantenimiento. */
@RestController
@RequestMapping("/api/operacion")
public class OperacionController {
    private final PersonalOperativoRepository personalRepo;
    private final EquipoMonitoreoRepository equipoRepo;
    private final IncidenciaUsuarioRepository incidenciaRepo;
    private final MantenimientoProgramadoRepository mantenimientoRepo;
    private final PuntoMonitoreoRepository puntoRepo;
    private final AuditoriaService auditoria;

    public OperacionController(PersonalOperativoRepository personalRepo, EquipoMonitoreoRepository equipoRepo,
                               IncidenciaUsuarioRepository incidenciaRepo, MantenimientoProgramadoRepository mantenimientoRepo,
                               PuntoMonitoreoRepository puntoRepo, AuditoriaService auditoria) {
        this.personalRepo = personalRepo; this.equipoRepo = equipoRepo; this.incidenciaRepo = incidenciaRepo;
        this.mantenimientoRepo = mantenimientoRepo; this.puntoRepo = puntoRepo; this.auditoria = auditoria;
    }

    // Equipos: datos descriptivos disponibles para todo usuario autenticado; escritura solo administrativa.
    @GetMapping("/equipos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','GERENTE')")
    public List<EquipoMonitoreo> equipos() { return equipoRepo.findAllByOrderByCodigoAsc(); }

    @PostMapping("/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EquipoMonitoreo crearEquipo(@Valid @RequestBody EquipoRequest r) {
        if (equipoRepo.existsByCodigoIgnoreCase(r.codigo().trim())) throw conflict("Ese código de equipo ya está registrado.");
        validarReferencias(r.idPunto(), r.idPersonalResponsable());
        EquipoMonitoreo e = new EquipoMonitoreo(); aplicar(e, r); return equipoRepo.save(e);
    }

    @PutMapping("/equipos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EquipoMonitoreo actualizarEquipo(@PathVariable Integer id, @Valid @RequestBody EquipoRequest r) {
        EquipoMonitoreo e = equipoRepo.findById(id).orElseThrow(() -> notFound("No se encontró el equipo."));
        if (equipoRepo.existsByCodigoIgnoreCaseAndIdEquipoNot(r.codigo().trim(), id)) throw conflict("Ese código ya pertenece a otro equipo.");
        validarReferencias(r.idPunto(), r.idPersonalResponsable()); aplicar(e, r); return equipoRepo.save(e);
    }

    @PatchMapping("/equipos/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EquipoMonitoreo estadoEquipo(@PathVariable Integer id, @RequestBody EstadoRequest r) {
        EquipoMonitoreo e = equipoRepo.findById(id).orElseThrow(() -> notFound("No se encontró el equipo."));
        if (!List.of("ACTIVO", "EN_MANTENIMIENTO", "INACTIVO", "DANADO").contains(normalizar(r.estado()))) throw badRequest("Estado de equipo no válido.");
        e.setEstado(normalizar(r.estado())); return equipoRepo.save(e);
    }

    @GetMapping("/personal")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<PersonalOperativo> personal() { return personalRepo.findAllByOrderByApellidosAscNombresAsc(); }

    @PostMapping("/personal")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PersonalOperativo crearPersonal(@Valid @RequestBody PersonalRequest r) {
        PersonalOperativo p = new PersonalOperativo(); aplicar(p, r); p.setDemo(false); return personalRepo.save(p);
    }

    @PutMapping("/personal/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PersonalOperativo actualizarPersonal(@PathVariable Integer id, @Valid @RequestBody PersonalRequest r) {
        PersonalOperativo p = personalRepo.findById(id).orElseThrow(() -> notFound("No se encontró al trabajador."));
        aplicar(p, r); return personalRepo.save(p);
    }

    @PatchMapping("/personal/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PersonalOperativo estadoPersonal(@PathVariable Integer id, @RequestBody EstadoRequest r) {
        PersonalOperativo p = personalRepo.findById(id).orElseThrow(() -> notFound("No se encontró al trabajador."));
        p.setEstado(estadoActivoInactivo(r.estado())); return personalRepo.save(p);
    }

    // Todo usuario autenticado puede enviar comentarios; solo el administrador consulta la bandeja completa.
    @GetMapping("/incidencias")
    public List<IncidenciaUsuario> incidencias(@AuthenticationPrincipal UsuarioPrincipal actual) {
        if (actual == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if ("ADMINISTRADOR".equals(actual.getRol())) return incidenciaRepo.findAllByOrderByFechaReporteDesc();
        return incidenciaRepo.findByIdUsuarioReportaOrderByFechaReporteDesc(actual.getIdUsuario());
    }

    @PostMapping("/incidencias")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaUsuario crearIncidencia(@Valid @RequestBody IncidenciaRequest r,
                                             @AuthenticationPrincipal UsuarioPrincipal actual) {
        if (actual == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (r.idEquipo() != null && !equipoRepo.existsById(r.idEquipo())) throw notFound("El equipo seleccionado no existe.");
        IncidenciaUsuario i = new IncidenciaUsuario();
        i.setCategoria(r.categoria().trim()); i.setTitulo(r.titulo().trim()); i.setZona(r.zona().trim());
        i.setDescripcion(r.descripcion().trim()); i.setEstado("PENDIENTE"); i.setPrioridad("MEDIA");
        i.setIdUsuarioReporta(actual.getIdUsuario()); i.setNombreReportante(actual.getNombreCompleto()); i.setIdEquipo(r.idEquipo());
        return incidenciaRepo.save(i);
    }

    @PatchMapping("/incidencias/{id}/gestionar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public IncidenciaUsuario gestionarIncidencia(@PathVariable Long id, @Valid @RequestBody GestionIncidenciaRequest r) {
        IncidenciaUsuario i = incidenciaRepo.findById(id).orElseThrow(() -> notFound("No se encontró el reporte."));
        var antes = new java.util.LinkedHashMap<String, Object>();
        antes.put("categoria", i.getCategoria()); antes.put("titulo", i.getTitulo()); antes.put("zona", i.getZona()); antes.put("descripcion", i.getDescripcion());
        antes.put("estado", i.getEstado()); antes.put("prioridad", i.getPrioridad()); antes.put("idEquipo", i.getIdEquipo());
        antes.put("idPersonalAsignado", i.getIdPersonalAsignado()); antes.put("fechaHoraRevision", i.getFechaHoraRevision()); antes.put("resultadoResolucion", i.getResultadoResolucion());
        if (r.estado() != null && !List.of("PENDIENTE", "EN_REVISION", "ASIGNADA", "ATENDIDA", "CERRADA", "REQUIERE_INFORMACION").contains(normalizar(r.estado()))) throw badRequest("Estado de reporte no válido.");
        if (r.prioridad() != null && !List.of("BAJA", "MEDIA", "ALTA", "CRITICA").contains(normalizar(r.prioridad()))) throw badRequest("Prioridad no válida.");
        if (r.idEquipo() != null && !equipoRepo.existsById(r.idEquipo())) throw notFound("El equipo seleccionado no existe.");
        if (r.idPersonalAsignado() != null) {
            PersonalOperativo p = personalRepo.findById(r.idPersonalAsignado()).orElseThrow(() -> notFound("No se encontró al responsable."));
            if (!"ACTIVO".equals(p.getEstado())) throw badRequest("No se puede asignar una incidencia a personal inactivo.");
        }
        if (r.estado() != null) i.setEstado(normalizar(r.estado()));
        if (r.prioridad() != null) i.setPrioridad(normalizar(r.prioridad()));
        if (r.categoria() != null && !r.categoria().isBlank()) i.setCategoria(r.categoria().trim());
        if (r.titulo() != null && !r.titulo().isBlank()) i.setTitulo(r.titulo().trim());
        if (r.zona() != null && !r.zona().isBlank()) i.setZona(r.zona().trim());
        if (r.descripcion() != null && !r.descripcion().isBlank()) i.setDescripcion(r.descripcion().trim());
        if (r.idEquipo() != null) i.setIdEquipo(r.idEquipo());
        if (r.idPersonalAsignado() != null) i.setIdPersonalAsignado(r.idPersonalAsignado());
        if (r.fechaHoraRevision() != null) i.setFechaHoraRevision(r.fechaHoraRevision());
        if (r.resultadoResolucion() != null) i.setResultadoResolucion(r.resultadoResolucion().trim());
        IncidenciaUsuario guardada = incidenciaRepo.save(i);
        var despues = new java.util.LinkedHashMap<String, Object>();
        despues.put("categoria", guardada.getCategoria()); despues.put("titulo", guardada.getTitulo()); despues.put("zona", guardada.getZona()); despues.put("descripcion", guardada.getDescripcion());
        despues.put("estado", guardada.getEstado()); despues.put("prioridad", guardada.getPrioridad()); despues.put("idEquipo", guardada.getIdEquipo());
        despues.put("idPersonalAsignado", guardada.getIdPersonalAsignado()); despues.put("fechaHoraRevision", guardada.getFechaHoraRevision()); despues.put("resultadoResolucion", guardada.getResultadoResolucion());
        auditoria.registrar(AccionAuditoria.MODIFICAR, "INCIDENCIA_USUARIO", id, "Actualización administrativa de observación #" + id, antes, despues);
        return guardada;
    }

    @GetMapping("/mantenimientos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR','GERENTE')")
    public List<MantenimientoProgramado> mantenimientos() { return mantenimientoRepo.findAllByOrderByFechaHoraProgramadaAsc(); }

    @PostMapping("/mantenimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MantenimientoProgramado crearMantenimiento(@Valid @RequestBody MantenimientoRequest r,
                                                       @AuthenticationPrincipal UsuarioPrincipal actual) {
        validarMantenimiento(r);
        MantenimientoProgramado m = new MantenimientoProgramado(); aplicar(m, r);
        m.setEstado("PROGRAMADO"); m.setIdUsuarioCreador(actual.getIdUsuario()); return mantenimientoRepo.save(m);
    }

    @PutMapping("/mantenimientos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MantenimientoProgramado actualizarMantenimiento(@PathVariable Long id, @Valid @RequestBody MantenimientoRequest r) {
        MantenimientoProgramado m = mantenimientoRepo.findById(id).orElseThrow(() -> notFound("No se encontró el mantenimiento."));
        validarMantenimiento(r); aplicar(m, r); return mantenimientoRepo.save(m);
    }

    @PatchMapping("/mantenimientos/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MantenimientoProgramado estadoMantenimiento(@PathVariable Long id, @Valid @RequestBody EstadoMantenimientoRequest r) {
        MantenimientoProgramado m = mantenimientoRepo.findById(id).orElseThrow(() -> notFound("No se encontró el mantenimiento."));
        String estado = normalizar(r.estado());
        if (!List.of("PROGRAMADO", "ASIGNADO", "EN_PROCESO", "COMPLETADO", "CANCELADO").contains(estado)) throw badRequest("Estado de mantenimiento no válido.");
        m.setEstado(estado); if (r.resultado() != null) m.setResultado(r.resultado().trim()); return mantenimientoRepo.save(m);
    }

    private void validarReferencias(Integer puntoId, Integer personalId) {
        if (puntoId != null && !puntoRepo.existsById(puntoId)) throw notFound("La estación seleccionada no existe.");
        if (personalId != null) {
            PersonalOperativo p = personalRepo.findById(personalId).orElseThrow(() -> notFound("No se encontró al responsable."));
            if (!"ACTIVO".equals(p.getEstado())) throw badRequest("El responsable seleccionado está inactivo.");
        }
    }
    private void validarMantenimiento(MantenimientoRequest r) {
        if (!equipoRepo.existsById(r.idEquipo())) throw notFound("El equipo seleccionado no existe.");
        PersonalOperativo p = personalRepo.findById(r.idPersonalAsignado()).orElseThrow(() -> notFound("No se encontró al responsable."));
        if (!"ACTIVO".equals(p.getEstado())) throw badRequest("El responsable seleccionado está inactivo.");
        if (r.fechaHoraProgramada().isBefore(LocalDateTime.now().minusMinutes(1))) throw badRequest("La fecha de mantenimiento no puede estar en el pasado.");
    }
    private void aplicar(EquipoMonitoreo e, EquipoRequest r) {
        e.setCodigo(r.codigo().trim().toUpperCase(Locale.ROOT)); e.setNombre(r.nombre().trim()); e.setTipoEquipo(r.tipoEquipo().trim());
        e.setFabricante(limpio(r.fabricante())); e.setModelo(limpio(r.modelo())); e.setModoLectura(normalizarModo(r.modoLectura()));
        e.setEstado(normalizar(r.estado())); e.setEstadoConexion(normalizar(r.estadoConexion())); e.setIdPunto(r.idPunto());
        e.setIdPersonalResponsable(r.idPersonalResponsable()); e.setUltimaRevision(r.ultimaRevision()); e.setProximaRevision(r.proximaRevision());
        e.setObservaciones(limpio(r.observaciones()));
    }
    private void aplicar(PersonalOperativo p, PersonalRequest r) {
        p.setNombres(r.nombres().trim()); p.setApellidos(r.apellidos().trim()); p.setCargo(r.cargo().trim()); p.setArea(r.area().trim());
        p.setTurno(r.turno().trim()); p.setTelefono(limpio(r.telefono())); p.setCorreo(limpio(r.correo()));
        if (r.estado() != null) p.setEstado(estadoActivoInactivo(r.estado()));
    }
    private void aplicar(MantenimientoProgramado m, MantenimientoRequest r) {
        m.setIdEquipo(r.idEquipo()); m.setIdPersonalAsignado(r.idPersonalAsignado()); m.setFechaHoraProgramada(r.fechaHoraProgramada());
        m.setTipo(r.tipo().trim()); m.setDescripcion(r.descripcion().trim());
        if (r.estado() != null) {
            String estado = normalizar(r.estado());
            if (!List.of("PROGRAMADO", "ASIGNADO", "EN_PROCESO", "COMPLETADO", "CANCELADO").contains(estado)) throw badRequest("Estado de mantenimiento no válido.");
            m.setEstado(estado);
        }
        m.setResultado(limpio(r.resultado()));
    }
    private String normalizarModo(String v) {
        String s = normalizar(v);
        if (!List.of("MANUAL", "AUTOMATICO", "SEMIAUTOMATICO").contains(s)) throw badRequest("Modo de lectura no válido.");
        return s;
    }
    private String estadoActivoInactivo(String v) {
        String s = normalizar(v); if (!List.of("ACTIVO", "INACTIVO").contains(s)) throw badRequest("Estado no válido."); return s;
    }
    private String normalizar(String v) { return v == null ? "" : v.trim().toUpperCase(Locale.ROOT); }
    private String limpio(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private ResponseStatusException notFound(String m) { return new ResponseStatusException(HttpStatus.NOT_FOUND, m); }
    private ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
    private ResponseStatusException badRequest(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    public record EstadoRequest(@NotBlank String estado) {}
    public record EquipoRequest(@NotBlank @Size(max=35) String codigo, @NotBlank @Size(max=130) String nombre,
        @NotBlank @Size(max=80) String tipoEquipo, @Size(max=100) String fabricante, @Size(max=100) String modelo,
        @NotBlank String modoLectura, @NotBlank String estado, @NotBlank String estadoConexion,
        Integer idPunto, Integer idPersonalResponsable, java.time.LocalDate ultimaRevision,
        java.time.LocalDate proximaRevision, @Size(max=1000) String observaciones) {}
    public record PersonalRequest(@NotBlank @Size(max=90) String nombres, @NotBlank @Size(max=90) String apellidos,
        @NotBlank @Size(max=100) String cargo, @NotBlank @Size(max=100) String area, @NotBlank @Size(max=80) String turno,
        @Size(max=25) String telefono, @Size(max=120) String correo, String estado) {}
    public record IncidenciaRequest(@NotBlank @Size(max=50) String categoria, @NotBlank @Size(max=140) String titulo,
        @NotBlank @Size(max=180) String zona, @NotBlank @Size(max=4000) String descripcion, Integer idEquipo) {}
    public record GestionIncidenciaRequest(String estado, String prioridad, Integer idEquipo, Integer idPersonalAsignado,
        LocalDateTime fechaHoraRevision, @Size(max=2000) String resultadoResolucion,
        @Size(max=50) String categoria, @Size(max=140) String titulo, @Size(max=180) String zona,
        @Size(max=4000) String descripcion) {}
    public record MantenimientoRequest(@jakarta.validation.constraints.NotNull Integer idEquipo,
        @jakarta.validation.constraints.NotNull Integer idPersonalAsignado,
        @jakarta.validation.constraints.NotNull LocalDateTime fechaHoraProgramada,
        @NotBlank @Size(max=60) String tipo, @NotBlank @Size(max=1500) String descripcion,
        String estado, @Size(max=2000) String resultado) {}
    public record EstadoMantenimientoRequest(@NotBlank String estado, @Size(max=2000) String resultado) {}
}
