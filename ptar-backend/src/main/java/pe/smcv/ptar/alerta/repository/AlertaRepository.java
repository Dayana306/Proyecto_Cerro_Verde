package pe.smcv.ptar.alerta.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.alerta.entity.Alerta;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.NivelAlerta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    String FETCH = """
            select a from Alerta a
              join fetch a.lectura l join fetch l.parametro pa
              join fetch l.monitoreo m join fetch m.punto p
              join fetch a.umbral u
            """;

    String FILTRO = """
            where (:estado is null or a.estado = :estado)
              and (:nivel is null or a.nivel = :nivel)
              and (:idPunto is null or p.idPunto = :idPunto)
              and (:idParametro is null or pa.idParametro = :idParametro)
              and (:desde is null or a.fechaDeteccion >= :desde)
              and (:hasta is null or a.fechaDeteccion <= :hasta)
            """;

    @Query(value = FETCH + FILTRO,
            countQuery = """
            select count(a) from Alerta a join a.lectura l join l.parametro pa join l.monitoreo m join m.punto p
            """ + FILTRO)
    Page<Alerta> buscar(@Param("estado") EstadoAlerta estado,
                        @Param("nivel") NivelAlerta nivel,
                        @Param("idPunto") Integer idPunto,
                        @Param("idParametro") Integer idParametro,
                        @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta,
                        Pageable pageable);

    @Query(FETCH + " where a.idAlerta = :id")
    Optional<Alerta> buscarCompleta(@Param("id") Integer id);

    /** Alertas de un periodo (reportes). */
    @Query(FETCH + """
            where a.fechaDeteccion between :desde and :hasta
              and (:idPunto is null or p.idPunto = :idPunto)
            order by a.fechaDeteccion
            """)
    List<Alerta> periodo(@Param("idPunto") Integer idPunto,
                         @Param("desde") LocalDateTime desde,
                         @Param("hasta") LocalDateTime hasta);

    /** Alertas no cerradas más recientes (dashboard). */
    @Query(FETCH + """
            where a.estado <> pe.smcv.ptar.alerta.entity.EstadoAlerta.CERRADA
              and (:idPunto is null or p.idPunto = :idPunto)
            order by a.fechaDeteccion desc
            """)
    List<Alerta> recientesNoCerradas(@Param("idPunto") Integer idPunto, Pageable pageable);

    @Query(FETCH + " where m.idMonitoreo = :idMonitoreo order by a.fechaDeteccion")
    List<Alerta> deMonitoreo(@Param("idMonitoreo") Integer idMonitoreo);

    /** ¿La lectura ya tiene una alerta sin cerrar? (evita duplicados al corregir valores). */
    @Query("""
            select count(a) from Alerta a
            where a.lectura.idLectura = :idLectura
              and a.estado <> pe.smcv.ptar.alerta.entity.EstadoAlerta.CERRADA
            """)
    long contarNoCerradasDeLectura(@Param("idLectura") Long idLectura);

    @Query("""
            select a from Alerta a
            where a.lectura.idLectura = :idLectura
              and a.estado <> pe.smcv.ptar.alerta.entity.EstadoAlerta.CERRADA
            """)
    List<Alerta> noCerradasDeLectura(@Param("idLectura") Long idLectura);

    /** [idPunto, nivel, cantidad] de alertas no cerradas (visor 2D). */
    @Query("""
            select p.idPunto, a.nivel, count(a) from Alerta a
              join a.lectura l join l.monitoreo m join m.punto p
            where a.estado <> pe.smcv.ptar.alerta.entity.EstadoAlerta.CERRADA
            group by p.idPunto, a.nivel
            """)
    List<Object[]> contarAbiertasPorPuntoYNivel();

    /** [nivel, cantidad] de alertas no cerradas (dashboard). */
    @Query("""
            select a.nivel, count(a) from Alerta a
              join a.lectura l join l.monitoreo m
            where a.estado <> pe.smcv.ptar.alerta.entity.EstadoAlerta.CERRADA
              and (:idPunto is null or m.punto.idPunto = :idPunto)
            group by a.nivel
            """)
    List<Object[]> contarNoCerradasPorNivel(@Param("idPunto") Integer idPunto);
}
