package pe.smcv.ptar.parametro.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.dto.UmbralDto;
import pe.smcv.ptar.parametro.service.UmbralService;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.util.List;

/** Umbrales de alerta (RF-06). Lectura: todos. Configuración: solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/umbrales")
public class UmbralController {

    private final UmbralService service;

    public UmbralController(UmbralService service) {
        this.service = service;
    }

    /** Por defecto solo los activos. estado=INACTIVO muestra el historial de versiones. */
    @GetMapping
    public List<UmbralDto.Response> listar(@RequestParam(required = false) Integer idParametro,
                                           @RequestParam(required = false) Integer idPunto,
                                           @RequestParam(defaultValue = "false") boolean soloGenerales,
                                           @RequestParam(defaultValue = "ACTIVO") Estado estado) {
        return service.listar(idParametro, idPunto, soloGenerales, estado);
    }

    @GetMapping("/{id}")
    public UmbralDto.Response obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    /** Si ya existe uno activo para el mismo parámetro y punto, queda reemplazado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UmbralDto.Response crear(@Valid @RequestBody UmbralDto.Request request,
                                   @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.crear(request, usuario.getIdUsuario());
    }

    /** Devuelve la NUEVA versión (con otro idUmbral); la anterior queda INACTIVO. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UmbralDto.Response actualizar(@PathVariable Integer id, @Valid @RequestBody UmbralDto.LimitesRequest request,
                                         @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.actualizar(id, request, usuario.getIdUsuario());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UmbralDto.Response cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
