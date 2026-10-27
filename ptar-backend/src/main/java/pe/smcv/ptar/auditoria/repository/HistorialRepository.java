package pe.smcv.ptar.auditoria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.entity.Historial;

import java.time.LocalDateTime;
import java.util.List;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

    String FILTRO = """
            from Historial h left join h.usuario u
            where (:entidad is null or h.entidad = :entidad)
              and (:accion is null or h.accion = :accion)
              and (:idUsuario is null or u.idUsuario = :idUsuario)
              and (:desde is null or h.fechaHora >= :desde)
              and (:hasta is null or h.fechaHora <= :hasta)
            """;

    @Query(value = "select h " + FILTRO, countQuery = "select count(h) " + FILTRO)
    Page<Historial> buscar(@Param("entidad") String entidad,
                           @Param("accion") AccionAuditoria accion,
                           @Param("idUsuario") Integer idUsuario,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pageable);

    @Query("select h " + FILTRO + " order by h.fechaHora")
    List<Historial> listar(@Param("entidad") String entidad,
                           @Param("accion") AccionAuditoria accion,
                           @Param("idUsuario") Integer idUsuario,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta);
}
