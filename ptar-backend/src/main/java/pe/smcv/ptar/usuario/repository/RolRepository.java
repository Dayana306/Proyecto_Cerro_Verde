package pe.smcv.ptar.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.usuario.entity.Rol;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(String nombre);
}
