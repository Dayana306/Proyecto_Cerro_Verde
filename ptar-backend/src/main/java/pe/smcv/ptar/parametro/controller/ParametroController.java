package pe.smcv.ptar.parametro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.dto.ParametroDto;
import pe.smcv.ptar.parametro.service.ParametroService;

import java.util.List;

/** Parámetros monitoreados. Lectura: cualquier usuario autenticado. Escritura: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/parametros")
public class ParametroController {

    private final ParametroService service;

    public ParametroController(ParametroService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParametroDto.Response> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @GetMapping("/{id}")
    public ParametroDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ParametroDto.Response crear(@Valid @RequestBody ParametroDto.Request request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ParametroDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody ParametroDto.Request request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ParametroDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
