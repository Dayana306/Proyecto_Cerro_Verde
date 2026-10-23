package pe.smcv.ptar.usuario.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.usuario.entity.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Integer idUsuario);

    boolean existsByUsuarioLoginIgnoreCase(String usuarioLogin);

    long countByRol_NombreAndEstado(String rol, Estado estado);

    /** Destinatarios de notificaciones de alerta. */
    List<Usuario> findByRol_NombreInAndEstado(Collection<String> roles, Estado estado);

    @Query(value = """
            select u from Usuario u join fetch u.rol r
            where (:idRol is null or r.idRol = :idRol)
              and (:estado is null or u.estado = :estado)
              and (:q is null or lower(concat(u.nombre, ' ', u.apellido, ' ', u.correo)) like lower(concat('%', :q, '%')))
            """,
            countQuery = """
            select count(u) from Usuario u join u.rol r
            where (:idRol is null or r.idRol = :idRol)
              and (:estado is null or u.estado = :estado)
              and (:q is null or lower(concat(u.nombre, ' ', u.apellido, ' ', u.correo)) like lower(concat('%', :q, '%')))
            """)
    Page<Usuario> buscar(@Param("q") String q,
                         @Param("idRol") Integer idRol,
                         @Param("estado") Estado estado,
                         Pageable pageable);
}
