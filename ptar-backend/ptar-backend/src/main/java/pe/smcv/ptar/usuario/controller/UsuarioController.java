package pe.smcv.ptar.usuario.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.common.dto.EstadoRequest;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.security.UsuarioPrincipal;
import pe.smcv.ptar.usuario.dto.PasswordResetRequest;
import pe.smcv.ptar.usuario.dto.UsuarioCreateRequest;
import pe.smcv.ptar.usuario.dto.UsuarioResponse;
import pe.smcv.ptar.usuario.dto.UsuarioUpdateRequest;
import pe.smcv.ptar.usuario.service.UsuarioService;

/** Gestión de usuarios (RF-09). Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Filtros opcionales: q (busca en nombre, apellido y correo), idRol, estado. */
    @GetMapping
    public PageResponse<UsuarioResponse> listar(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) Integer idRol,
                                                @RequestParam(required = false) Estado estado,
                                                @PageableDefault(size = 20, sort = "idUsuario") Pageable pageable) {
        return usuarioService.listar(q, idRol, estado, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Integer id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioUpdateRequest request,
                                      @AuthenticationPrincipal UsuarioPrincipal actual) {
        return usuarioService.actualizar(id, request, actual.getIdUsuario());
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Integer id,
                                         @Valid @RequestBody EstadoRequest request,
                                         @AuthenticationPrincipal UsuarioPrincipal actual) {
        return usuarioService.cambiarEstado(id, request.estado(), actual.getIdUsuario());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerPassword(@PathVariable Integer id, @Valid @RequestBody PasswordResetRequest request) {
        usuarioService.restablecerPassword(id, request.passwordNueva());
    }
}
