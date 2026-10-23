package pe.smcv.ptar.ubicacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;

import java.util.List;

public interface PuntoMonitoreoRepository extends JpaRepository<PuntoMonitoreo, Integer> {

    @Query("""
            select p from PuntoMonitoreo p join fetch p.zona z join fetch p.area a
            where (:idZona is null or z.idZona = :idZona)
              and (:idArea is null or a.idArea = :idArea)
              and (:estado is null or p.estado = :estado)
            order by p.nombrePunto
            """)
    List<PuntoMonitoreo> buscar(@Param("idZona") Integer idZona,
                                @Param("idArea") Integer idArea,
                                @Param("estado") Estado estado);

    long countByEstado(Estado estado);
    boolean existsByNombrePuntoIgnoreCase(String nombre);
    boolean existsByNombrePuntoIgnoreCaseAndIdPuntoNot(String nombre, Integer id);
}
