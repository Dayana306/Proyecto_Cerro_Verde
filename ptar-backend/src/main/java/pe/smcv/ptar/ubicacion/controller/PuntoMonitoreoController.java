package pe.smcv.ptar.ubicacion.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.dto.PuntoDto;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;

import java.util.List;

/** Puntos (estaciones) de monitoreo. Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/puntos-monitoreo")
public class PuntoMonitoreoController {

    private final PuntoMonitoreoService service;

    public PuntoMonitoreoController(PuntoMonitoreoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PuntoDto.Response> listar(@RequestParam(required = false) Integer idZona,
                                          @RequestParam(required = false) Integer idArea,
                                          @RequestParam(required = false) Estado estado) {
        return service.listar(idZona, idArea, estado);
    }

    @GetMapping("/{id}")
    public PuntoDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PuntoDto.Response crear(@Valid @RequestBody PuntoDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PuntoDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody PuntoDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PuntoDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
