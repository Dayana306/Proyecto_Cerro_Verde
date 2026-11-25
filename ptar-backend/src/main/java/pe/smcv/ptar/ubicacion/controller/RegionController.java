package pe.smcv.ptar.ubicacion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.dto.RegionDto;
import pe.smcv.ptar.ubicacion.service.RegionService;

import java.util.List;

/** Regiones. Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/regiones")
public class RegionController {

    private final RegionService service;

    public RegionController(RegionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegionDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public RegionDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RegionDto.Response crear(@Valid @RequestBody RegionDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RegionDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody RegionDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RegionDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
