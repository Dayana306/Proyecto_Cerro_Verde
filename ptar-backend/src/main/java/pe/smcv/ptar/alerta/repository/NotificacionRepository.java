package pe.smcv.ptar.alerta.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.entity.Notificacion;

import java.time.LocalDateTime;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    @Query(value = """
            select n from Notificacion n join fetch n.alerta a
            where n.usuario.idUsuario = :idUsuario
              and (:soloNoLeidas = false or n.fechaLectura is null)
            """,
            countQuery = """
            select count(n) from Notificacion n
            where n.usuario.idUsuario = :idUsuario
              and (:soloNoLeidas = false or n.fechaLectura is null)
            """)
    Page<Notificacion> deUsuario(@Param("idUsuario") Integer idUsuario,
                                 @Param("soloNoLeidas") boolean soloNoLeidas,
                                 Pageable pageable);

    long countByUsuario_IdUsuarioAndFechaLecturaIsNull(Integer idUsuario);

    @Modifying
    @Transactional
    @Query("""
            update Notificacion n set n.fechaLectura = :ahora
            where n.usuario.idUsuario = :idUsuario and n.fechaLectura is null
            """)
    int marcarTodasLeidas(@Param("idUsuario") Integer idUsuario, @Param("ahora") LocalDateTime ahora);
}
