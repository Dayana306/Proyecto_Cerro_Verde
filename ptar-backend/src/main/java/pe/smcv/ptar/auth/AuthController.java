package pe.smcv.ptar.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.smcv.ptar.auth.dto.CambioPasswordRequest;
import pe.smcv.ptar.auth.dto.ConfirmarRecuperacionRequest;
import pe.smcv.ptar.auth.dto.LoginRequest;
import pe.smcv.ptar.auth.dto.LoginResponse;
import pe.smcv.ptar.auth.dto.SesionResponse;
import pe.smcv.ptar.auth.dto.SolicitarRecuperacionRequest;
import pe.smcv.ptar.security.UsuarioPrincipal;
import pe.smcv.ptar.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, UsuarioService usuarioService,
                          PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    /** Público. Devuelve el token JWT y los datos básicos del usuario. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Usuario de la sesión actual; el frontend lo usa para decidir qué pantallas mostrar según el rol. */
    @GetMapping("/me")
    public SesionResponse me(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return authService.aSesion(usuario);
    }

    /** Solicita recuperación por correo; la respuesta no revela si la cuenta existe. */
    @PostMapping("/password/solicitar-recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        passwordRecoveryService.solicitar(request.correo());
    }

    /** Confirma el cambio con el token de un solo uso recibido por correo. */
    @PostMapping("/password/confirmar-recuperacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionRequest request) {
        passwordRecoveryService.confirmar(request.token(), request.passwordNueva());
    }

    /** Cualquier usuario autenticado puede cambiar su propia contraseña. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                @Valid @RequestBody CambioPasswordRequest request) {
        usuarioService.cambiarPasswordPropia(usuario.getIdUsuario(), request.passwordActual(), request.passwordNueva());
    }
}
