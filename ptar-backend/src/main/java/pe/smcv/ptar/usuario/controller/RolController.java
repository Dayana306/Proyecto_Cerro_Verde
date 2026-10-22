package pe.smcv.ptar.usuario.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smcv.ptar.usuario.dto.RolResponse;
import pe.smcv.ptar.usuario.service.UsuarioService;

import java.util.List;

//lista de roles activos, para el selector del formulario de usuarios
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolController {

    private final UsuarioService usuarioService;

    public RolController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<RolResponse> listar() {
        return usuarioService.listarRoles();
    }
}
