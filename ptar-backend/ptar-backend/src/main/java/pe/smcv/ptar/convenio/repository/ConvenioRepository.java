package pe.smcv.ptar.convenio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.entity.Convenio;

import java.util.List;

public interface ConvenioRepository extends JpaRepository<Convenio, Integer> {

    @Query("""
            select c from Convenio c join fetch c.entidad left join fetch c.punto
            where (:estado is null or c.estado = :estado)
            order by c.fechaInicio desc
            """)
    List<Convenio> buscar(@Param("estado") Estado estado);
}
