package pe.smcv.ptar.ubicacion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.dto.Mapa2dDto;
import pe.smcv.ptar.ubicacion.dto.MapaVisorResponse;
import pe.smcv.ptar.ubicacion.service.Mapa2dService;

import java.util.List;

/** Mapas base del visor 2D. Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/mapas")
public class Mapa2dController {

    private final Mapa2dService service;

    public Mapa2dController(Mapa2dService service) {
        this.service = service;
    }

    /** RF-08: mapa base activo + zonas + puntos con estado de alertas, listo para el visor 2D. */
    @GetMapping("/visor")
    public MapaVisorResponse visor() {
        return service.visor();
    }

    @GetMapping
    public List<Mapa2dDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public Mapa2dDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mapa2dDto.Response crear(@Valid @RequestBody Mapa2dDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mapa2dDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody Mapa2dDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Mapa2dDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
