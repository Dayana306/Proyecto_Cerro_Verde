package pe.smcv.ptar.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.util.Collection;
import java.util.List;

/** Usuario autenticado. Se obtiene en los controladores con @AuthenticationPrincipal. */
public class UsuarioPrincipal implements UserDetails {

    private final Integer idUsuario;
    private final String correo;
    private final String nombreCompleto;
    private final String rol;
    private final String passwordHash;
    private final boolean activo;

    public UsuarioPrincipal(Integer idUsuario, String correo, String nombreCompleto,
                            String rol, String passwordHash, boolean activo) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.passwordHash = passwordHash;
        this.activo = activo;
    }

    public static UsuarioPrincipal desde(Usuario u) {
        return new UsuarioPrincipal(
                u.getIdUsuario(),
                u.getCorreo(),
                u.getNombreCompleto(),
                u.getRol().getNombre(),
                u.getPasswordHash(),
                u.getEstado() == Estado.ACTIVO && u.getRol().getEstado() == Estado.ACTIVO);
    }

    public Integer getIdUsuario() { return idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRol() { return rol; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return correo; }
    @Override public boolean isEnabled() { return activo; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
