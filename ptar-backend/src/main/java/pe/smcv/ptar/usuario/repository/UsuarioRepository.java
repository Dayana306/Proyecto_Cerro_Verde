package pe.smcv.ptar.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByUsuarioLogin(String usuarioLogin);
}
