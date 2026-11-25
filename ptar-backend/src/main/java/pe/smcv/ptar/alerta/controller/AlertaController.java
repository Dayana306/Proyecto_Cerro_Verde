package pe.smcv.ptar.alerta.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.alerta.dto.SeguimientoRequest;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.alerta.service.AlertaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.time.LocalDate;

/** Consulta y seguimiento de alertas (RF-05, CU09). Las alertas las crea el sistema, no la API. */
@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AlertaResponse> buscar(
            @RequestParam(required = false) EstadoAlerta estado,
            @RequestParam(required = false) NivelAlerta nivel,
            @RequestParam(required = false) Integer idPunto,
            @RequestParam(required = false) Integer idParametro,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaDeteccion", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(estado, nivel, idPunto, idParametro, desde, hasta, pageable);
    }

    /** Detalle con el historial de seguimiento. */
    @GetMapping("/{id}")
    public AlertaResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    /** Comentario + cambio de estado opcional (ABIERTA, EN_REVISION, CERRADA). */
    @PostMapping("/{id}/seguimientos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE','SUPERVISOR')")
    public AlertaResponse agregarSeguimiento(@PathVariable Integer id, @Valid @RequestBody SeguimientoRequest request,
                                             @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.agregarSeguimiento(id, request, usuario.getIdUsuario());
    }
}
