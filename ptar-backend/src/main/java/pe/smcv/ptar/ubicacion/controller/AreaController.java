package pe.smcv.ptar.ubicacion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.dto.AreaDto;
import pe.smcv.ptar.ubicacion.service.AreaService;

import java.util.List;

/** Áreas organizacionales. Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AreaDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public AreaDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaDto.Response crear(@Valid @RequestBody AreaDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody AreaDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AreaDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
