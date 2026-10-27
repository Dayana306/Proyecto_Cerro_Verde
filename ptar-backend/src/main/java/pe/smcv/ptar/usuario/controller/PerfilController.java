package pe.smcv.ptar.usuario.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.security.UsuarioPrincipal;
import pe.smcv.ptar.usuario.dto.PerfilResponse;
import pe.smcv.ptar.usuario.dto.PerfilUpdateRequest;
import pe.smcv.ptar.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {
    private final UsuarioService usuarios;
    public PerfilController(UsuarioService usuarios){this.usuarios=usuarios;}
    @GetMapping public PerfilResponse obtener(@AuthenticationPrincipal UsuarioPrincipal actual){return usuarios.obtenerPerfil(actual.getIdUsuario());}
    @PutMapping public PerfilResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal actual, @Valid @RequestBody PerfilUpdateRequest request){return usuarios.actualizarPerfil(actual.getIdUsuario(),request);}
}
