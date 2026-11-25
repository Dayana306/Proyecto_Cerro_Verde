package pe.smcv.ptar.ubicacion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.dto.ZonaDto;
import pe.smcv.ptar.ubicacion.service.ZonaService;

import java.util.List;

/** Zonas de afectación (RF-08). Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService service;

    public ZonaController(ZonaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ZonaDto.Response> listar(@RequestParam(required = false) Integer idRegion, @RequestParam(required = false) Estado estado) {
        return service.listar(idRegion, estado);
    }

    @GetMapping("/{id}")
    public ZonaDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ZonaDto.Response crear(@Valid @RequestBody ZonaDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ZonaDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody ZonaDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ZonaDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
