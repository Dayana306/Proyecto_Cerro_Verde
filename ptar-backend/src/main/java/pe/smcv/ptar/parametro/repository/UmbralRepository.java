package pe.smcv.ptar.parametro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.entity.Umbral;

import java.util.List;

public interface UmbralRepository extends JpaRepository<Umbral, Integer> {

    @Query("""
            select u from Umbral u join fetch u.parametro p left join fetch u.punto pt join fetch u.usuario us
            where (:idParametro is null or p.idParametro = :idParametro)
              and (:idPunto is null or pt.idPunto = :idPunto)
              and (:soloGenerales = false or pt is null)
              and (:estado is null or u.estado = :estado)
            order by p.nombreParametro, u.fechaCreacion desc
            """)
    List<Umbral> buscar(@Param("idParametro") Integer idParametro,
                        @Param("idPunto") Integer idPunto,
                        @Param("soloGenerales") boolean soloGenerales,
                        @Param("estado") Estado estado);

    /** Umbral activo específico de un punto. */
    @Query("""
            select u from Umbral u
            where u.parametro.idParametro = :idParametro and u.punto.idPunto = :idPunto and u.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO
            order by u.fechaCreacion desc
            """)
    List<Umbral> activosDePunto(@Param("idParametro") Integer idParametro, @Param("idPunto") Integer idPunto);

    /** Umbral activo general (sin punto). */
    @Query("""
            select u from Umbral u
            where u.parametro.idParametro = :idParametro and u.punto is null and u.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO
            order by u.fechaCreacion desc
            """)
    List<Umbral> activosGenerales(@Param("idParametro") Integer idParametro);
}
