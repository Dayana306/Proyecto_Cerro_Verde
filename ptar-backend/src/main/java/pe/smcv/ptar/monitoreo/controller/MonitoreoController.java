package pe.smcv.ptar.monitoreo.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.monitoreo.dto.DocumentoResponse;
import pe.smcv.ptar.monitoreo.dto.MonitoreoRequest;
import pe.smcv.ptar.monitoreo.dto.MonitoreoResponse;
import pe.smcv.ptar.monitoreo.service.DocumentoService;
import pe.smcv.ptar.monitoreo.service.MonitoreoService;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.util.List;

/** Registro y consulta de mediciones (RF-02, RF-03). Escritura: SUPERVISOR y ADMINISTRADOR. */
@RestController
@RequestMapping("/api/monitoreos")
public class MonitoreoController {

    private static final String PUEDE_REGISTRAR = "hasAnyRole('ADMINISTRADOR','SUPERVISOR')";

    private final MonitoreoService service;
    private final DocumentoService documentoService;

    public MonitoreoController(MonitoreoService service, DocumentoService documentoService) {
        this.service = service;
        this.documentoService = documentoService;
    }

    /** Historial (CU03). Por defecto incluye anulados; usar estado=ACTIVO para ocultarlos. */
    @GetMapping
    public PageResponse<MonitoreoResponse> buscar(
            @RequestParam(required = false) Integer idPunto,
            @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "horaMuestreo", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(idPunto, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    public MonitoreoResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    /** Respuesta 201 con el monitoreo y las alertas que generó. 422 si algún valor está fuera de rango. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PUEDE_REGISTRAR)
    public MonitoreoResponse registrar(@Valid @RequestBody MonitoreoRequest request,
                                       @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.registrar(request, usuario.getIdUsuario());
    }

    @PutMapping("/{id}")
    @PreAuthorize(PUEDE_REGISTRAR)
    public MonitoreoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody MonitoreoRequest request,
                                        @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.actualizar(id, request, usuario.getIdUsuario());
    }

    /** INACTIVO = anular (cierra sus alertas abiertas). */
    @PatchMapping("/{id}/estado")
    @PreAuthorize(PUEDE_REGISTRAR)
    public MonitoreoResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request,
                                           @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.cambiarEstado(id, request.estado(), usuario.getIdUsuario());
    }

    // ---------------- documentos del monitoreo ----------------

    @GetMapping("/{id}/documentos")
    public List<DocumentoResponse> documentos(@PathVariable Integer id,
                                              @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return documentoService.deMonitoreo(id, incluirInactivos);
    }

    /** multipart/form-data con los campos "archivo" (obligatorio) y "descripcion" (opcional). Máx. 10 MB. */
    @PostMapping(path = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PUEDE_REGISTRAR)
    public DocumentoResponse subirDocumento(@PathVariable Integer id,
                                            @RequestParam("archivo") MultipartFile archivo,
                                            @RequestParam(value = "descripcion", required = false) String descripcion,
                                            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return documentoService.subir(id, archivo, descripcion, usuario.getIdUsuario());
    }
}
