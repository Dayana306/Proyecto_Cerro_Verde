package pe.smcv.ptar.monitoreo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.monitoreo.entity.Monitoreo;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {

    String FILTRO = """
            where (:idPunto is null or p.idPunto = :idPunto)
              and (:estado is null or m.estado = :estado)
              and (:desde is null or m.horaMuestreo >= :desde)
              and (:hasta is null or m.horaMuestreo <= :hasta)
            """;

    @Query(value = "select m from Monitoreo m join fetch m.punto p join fetch m.usuario u " + FILTRO,
            countQuery = "select count(m) from Monitoreo m join m.punto p " + FILTRO)
    Page<Monitoreo> buscar(@Param("idPunto") Integer idPunto,
                           @Param("estado") Estado estado,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pageable);

    @Query("""
            select distinct m from Monitoreo m
              join fetch m.punto join fetch m.usuario
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
