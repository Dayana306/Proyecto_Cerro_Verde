package pe.smcv.ptar.alerta.publicada;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/alertas-publicadas")
public class AlertaPublicadaController {
    private final AlertaPublicadaService service;

    public AlertaPublicadaController(AlertaPublicadaService service) { this.service = service; }

    @GetMapping
    public List<AlertaPublicadaResponse> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.listar(usuario.getIdUsuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertaPublicadaResponse crear(@Valid @RequestBody AlertaPublicadaRequest request,
                                         @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.crear(request, usuario.getIdUsuario());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AlertaPublicadaResponse actualizar(@PathVariable Integer id,
                                               @Valid @RequestBody AlertaPublicadaRequest request,
                                               @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.actualizar(id, request, usuario.getIdUsuario());
    }

    @PatchMapping("/{id}/vista")
    public AlertaPublicadaResponse marcarVista(@PathVariable Integer id,
                                                @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return service.marcarVista(id, usuario.getIdUsuario());
    }
}
