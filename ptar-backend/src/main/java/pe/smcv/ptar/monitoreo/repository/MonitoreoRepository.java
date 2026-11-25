package pe.smcv.ptar.monitoreo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.monitoreo.entity.Monitoreo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {

    String FILTRO = """
            where (:idPunto is null or p.idPunto = :idPunto)
              and (:estado is null or m.estado = :estado)
              and (:desde is null or m.horaMuestreo >= :desde)
              and (:hasta is null or m.horaMuestreo <= :hasta)
            """;

    /** Consulta general paginada con filtros opcionales. */
    @Query(value = "select m.idMonitoreo from Monitoreo m left join m.punto p " + FILTRO,
            countQuery = "select count(m) from Monitoreo m left join m.punto p " + FILTRO)
    Page<Integer> buscarIds(@Param("idPunto") Integer idPunto,
                            @Param("estado") Estado estado,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            Pageable pageable);

    /** Consulta sin filtros opcionales para que PostgreSQL no tenga que inferir el tipo de parámetros nulos. */
    @Query(value = "select m.idMonitoreo from Monitoreo m order by m.horaMuestreo desc, m.idMonitoreo desc",
            countQuery = "select count(m) from Monitoreo m")
    Page<Integer> buscarIdsRecientes(Pageable pageable);

    /** Recupera en una segunda consulta los monitoreos de la página con todas sus lecturas y parámetros. */
    @Query("""
            select distinct m from Monitoreo m
              left join fetch m.punto
              left join fetch m.usuario
              left join fetch m.lecturas l
              left join fetch l.parametro
            where m.idMonitoreo in :ids
            """)
    List<Monitoreo> buscarConLecturas(@Param("ids") Collection<Integer> ids);

    @Query("""
            select distinct m from Monitoreo m
              left join fetch m.punto left join fetch m.usuario
              left join fetch m.lecturas l left join fetch l.parametro
            where m.idMonitoreo = :id
            """)
    Optional<Monitoreo> buscarCompleto(@Param("id") Integer id);

    @Query("""
            select count(m) from Monitoreo m
            where m.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO
              and m.horaMuestreo between :desde and :hasta
              and (:idPunto is null or m.punto.idPunto = :idPunto)
            """)
    long contarActivos(@Param("idPunto") Integer idPunto,
                       @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta);
}
