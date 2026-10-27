package pe.smcv.ptar.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

/** Carga al usuario por correo (HU-01: autenticación con correo y contraseña). */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(UsuarioPrincipal::desde)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
