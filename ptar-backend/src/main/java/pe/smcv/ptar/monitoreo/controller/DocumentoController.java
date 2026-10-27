package pe.smcv.ptar.monitoreo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.util.Descargas;
import pe.smcv.ptar.monitoreo.dto.DocumentoResponse;
import pe.smcv.ptar.monitoreo.service.DocumentoService;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    /** Descarga con Authorization: Bearer (el frontend debe pedirlo con fetch/axios y crear un blob). */
    @GetMapping("/{id}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id) {
        DocumentoService.Archivo a = service.descargar(id);
        return Descargas.adjunto(a.contenido(), a.nombre(), a.tipoContenido());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERVISOR')")
    public DocumentoResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
