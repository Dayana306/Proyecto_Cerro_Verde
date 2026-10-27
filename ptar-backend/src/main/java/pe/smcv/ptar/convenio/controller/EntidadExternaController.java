package pe.smcv.ptar.convenio.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.dto.EntidadExternaDto;
import pe.smcv.ptar.convenio.service.EntidadExternaService;

import java.util.List;

/** Entidades externas (SEDAPAR, ANA, OEFA...). Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/entidades-externas")
public class EntidadExternaController {

    private final EntidadExternaService service;

    public EntidadExternaController(EntidadExternaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EntidadExternaDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public EntidadExternaDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EntidadExternaDto.Response crear(@Valid @RequestBody EntidadExternaDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EntidadExternaDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody EntidadExternaDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public EntidadExternaDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
