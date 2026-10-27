package pe.smcv.ptar.auditoria.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.smcv.ptar.auditoria.dto.HistorialResponse;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaExportService;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.util.Descargas;

import java.time.LocalDate;

/** Consulta y exportación de la bitácora (CU10/CU11). Solo ADMINISTRADOR y AUDITOR. */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR')")
public class AuditoriaController {

    private final AuditoriaService service;
    private final AuditoriaExportService exportService;

    public AuditoriaController(AuditoriaService service, AuditoriaExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping
    public PageResponse<HistorialResponse> buscar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 50, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(entidad, accion, idUsuario, desde, hasta, pageable);
    }

    /** Evidencias para auditoría externa: Excel con la bitácora filtrada. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] excel = exportService.exportarExcel(entidad, accion, idUsuario, desde, hasta);
        return Descargas.adjunto(excel, "bitacora_auditoria.xlsx", Descargas.XLSX);
    }
}
