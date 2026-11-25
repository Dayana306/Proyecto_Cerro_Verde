package pe.smcv.ptar.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.auth.dto.LoginRequest;
import pe.smcv.ptar.auth.dto.LoginResponse;
import pe.smcv.ptar.auth.dto.SesionResponse;
import pe.smcv.ptar.security.JwtService;
import pe.smcv.ptar.security.UsuarioPrincipal;

/** Autenticación con correo y contraseña (RF-01 / HU-01). */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditoriaService auditoria;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, AuditoriaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
    }

    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, request.password()));
        } catch (AuthenticationException e) {
            auditoria.registrarComo(null, AccionAuditoria.LOGIN_FALLIDO, "USUARIO", null,
                    "Intento de ingreso fallido: " + correo, null, null);
            throw e; // -> 401 en GlobalExceptionHandler
        }

        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();
        auditoria.registrarComo(usuario.getIdUsuario(), AccionAuditoria.LOGIN, "USUARIO", usuario.getIdUsuario(),
                "Ingreso al sistema", null, null);
        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(), aSesion(usuario));
    }

    public SesionResponse aSesion(UsuarioPrincipal u) {
        return new SesionResponse(u.getIdUsuario(), u.getNombreCompleto(), u.getUsername(), u.getRol());
    }
}
