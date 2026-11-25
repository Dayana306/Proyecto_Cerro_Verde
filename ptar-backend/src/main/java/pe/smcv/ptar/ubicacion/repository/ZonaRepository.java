package pe.smcv.ptar.ubicacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Zona;

import java.util.List;

public interface ZonaRepository extends JpaRepository<Zona, Integer> {

    @Query("""
            select z from Zona z join fetch z.region r
            where (:idRegion is null or r.idRegion = :idRegion)
              and (:estado is null or z.estado = :estado)
            order by z.nombreZona
            """)
    List<Zona> buscar(@Param("idRegion") Integer idRegion, @Param("estado") Estado estado);

    boolean existsByNombreZonaIgnoreCase(String nombre);
    boolean existsByNombreZonaIgnoreCaseAndIdZonaNot(String nombre, Integer id);
}
