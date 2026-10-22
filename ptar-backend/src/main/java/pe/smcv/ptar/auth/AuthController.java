package pe.smcv.ptar.auth;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smcv.ptar.auth.dto.LoginRequest;
import pe.smcv.ptar.auth.dto.LoginResponse;
import pe.smcv.ptar.auth.dto.SesionResponse;
import pe.smcv.ptar.security.UsuarioPrincipal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //pblic devuelve el token JWT y los datos basicos del usuario
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    //usuario de la sesión actual: el frontend sabrá qué pantallas mostrar según el rol
    @GetMapping("/me")
    public SesionResponse me(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return authService.aSesion(usuario);
    }
}
