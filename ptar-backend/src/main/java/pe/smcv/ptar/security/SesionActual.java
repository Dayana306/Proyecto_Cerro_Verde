package pe.smcv.ptar.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Acceso estático al usuario autenticado de la petición actual (si existe). */
public final class SesionActual {

    private SesionActual() {
    }

    public static Optional<UsuarioPrincipal> usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static Integer idUsuarioOrNull() {
        return usuario().map(UsuarioPrincipal::getIdUsuario).orElse(null);
    }
}
