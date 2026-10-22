package pe.smcv.ptar.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pe.smcv.ptar.auth.dto.LoginRequest;
import pe.smcv.ptar.auth.dto.LoginResponse;
import pe.smcv.ptar.auth.dto.SesionResponse;
import pe.smcv.ptar.security.JwtService;
import pe.smcv.ptar.security.UsuarioPrincipal;

//autenticación con correo y contraseña
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        //inicia authenticationexception si las credenciales son invalidas o la cuenta esta inactiva
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo().trim(), request.password()));

        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();
        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionSegundos(), aSesion(usuario));
    }

    public SesionResponse aSesion(UsuarioPrincipal u) {
        return new SesionResponse(u.getIdUsuario(), u.getNombreCompleto(), u.getUsername(), u.getRol());
    }
}
