package pe.smcv.ptar.monitoreo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;

import java.time.LocalDateTime;
import java.util.List;

/** Consultas de series de datos. Solo considera lecturas y monitoreos ACTIVOS. */
public interface LecturaRepository extends JpaRepository<LecturaParametro, Long> {

    String ACTIVAS = """
            l.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO
            and m.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO
            """;

    /** Serie de un parámetro (para gráficos y el convenio). idPunto null = todos los puntos. */
    @Query("select l from LecturaParametro l join fetch l.monitoreo m join fetch m.punto p join fetch l.parametro pa "
            + "where " + ACTIVAS + """
              and pa.idParametro = :idParametro
              and (:idPunto is null or p.idPunto = :idPunto)
              and m.horaMuestreo between :desde and :hasta
            order by m.horaMuestreo
            """)
    List<LecturaParametro> serie(@Param("idParametro") Integer idParametro,
                                 @Param("idPunto") Integer idPunto,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);

    /** Todas las lecturas de un periodo (reportes). */
    @Query("select l from LecturaParametro l join fetch l.monitoreo m join fetch m.punto p join fetch l.parametro pa "
            + "where " + ACTIVAS + """
              and (:idPunto is null or p.idPunto = :idPunto)
              and m.horaMuestreo between :desde and :hasta
            order by m.horaMuestreo, p.nombrePunto, pa.nombreParametro
            """)
    List<LecturaParametro> periodo(@Param("idPunto") Integer idPunto,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);

    /** Últimas lecturas de un parámetro (usar PageRequest.of(0, 1) para la más reciente). */
    @Query("select l from LecturaParametro l join fetch l.monitoreo m join fetch m.punto p join fetch l.parametro pa "
            + "where " + ACTIVAS + """
              and pa.idParametro = :idParametro
              and (:idPunto is null or p.idPunto = :idPunto)
            order by m.horaMuestreo desc, l.idLectura desc
            """)
    List<LecturaParametro> ultimas(@Param("idParametro") Integer idParametro,
                                   @Param("idPunto") Integer idPunto,
                                   Pageable pageable);
}
