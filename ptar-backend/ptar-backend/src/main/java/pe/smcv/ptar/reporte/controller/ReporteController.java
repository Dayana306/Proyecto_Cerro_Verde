package pe.smcv.ptar.reporte.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.util.Descargas;
import pe.smcv.ptar.reporte.dto.EnvioRequest;
import pe.smcv.ptar.reporte.dto.EnvioResponse;
import pe.smcv.ptar.reporte.dto.ReporteRequest;
import pe.smcv.ptar.reporte.dto.ReporteResponse;
import pe.smcv.ptar.reporte.entity.EstadoReporte;
import pe.smcv.ptar.reporte.entity.TipoReporte;
import pe.smcv.ptar.reporte.service.ReporteService;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.util.List;

/** Reportes (RF-07). Generan: ADMINISTRADOR, GERENTE y AUDITOR. El SUPERVISOR solo consulta. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final String PUEDE_GENERAR = "hasAnyRole('ADMINISTRADOR','GERENTE','AUDITOR')";

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ReporteResponse> buscar(
            @RequestParam(required = false) TipoReporte tipo,
            @RequestParam(required = false) EstadoReporte estado,
            @PageableDefault(size = 20, sort = "fechaGeneracion", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.buscar(tipo, estado, pageable);
    }

    @GetMapping("/{id}")
    public ReporteResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    /** Genera y guarda el reporte. Luego se descarga con GET /{id}/descarga. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(PUEDE_GENERAR)
    public ReporteResponse generar(@Valid @RequestBody ReporteRequest request,
                                   @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.generar(request, usuario.getIdUsuario());
    }

    @GetMapping("/{id}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id) {
        ReporteService.Archivo a = service.descargar(id);
        return Descargas.adjunto(a.contenido(), a.nombre(), a.tipoContenido());
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    public ReporteResponse anular(@PathVariable Integer id) {
        return service.anular(id);
    }

    @GetMapping("/{id}/envios")
    public List<EnvioResponse> envios(@PathVariable Integer id) {
        return service.envios(id);
    }

    /** Registra la entrega del reporte a una entidad externa. */
    @PostMapping("/{id}/envios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    public EnvioResponse registrarEnvio(@PathVariable Integer id, @Valid @RequestBody EnvioRequest request,
                                        @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.registrarEnvio(id, request, usuario.getIdUsuario());
    }
}
