package pe.smcv.ptar.convenio.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.dto.ConvenioDto;
import pe.smcv.ptar.convenio.dto.CumplimientoResponse;
import pe.smcv.ptar.convenio.service.ConvenioService;

import java.util.List;

/** Convenio hídrico (CU10). Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/convenios")
public class ConvenioController {

    private final ConvenioService service;

    public ConvenioController(ConvenioService service) {
        this.service = service;
    }

    /** CU10: cumplimiento de la meta de caudal en el año indicado (por defecto el actual). */
    @GetMapping("/{id}/cumplimiento")
    public CumplimientoResponse cumplimiento(@PathVariable Integer id, @RequestParam(required = false) Integer anio) {
        return service.cumplimiento(id, anio);
    }

    @GetMapping
    public List<ConvenioDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public ConvenioDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConvenioDto.Response crear(@Valid @RequestBody ConvenioDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConvenioDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody ConvenioDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConvenioDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
