package pe.smcv.ptar.ubicacion.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Region;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Integer> {
    List<Region> findByEstado(Estado estado, Sort sort);
    boolean existsByNombreRegionIgnoreCase(String nombre);
    boolean existsByNombreRegionIgnoreCaseAndIdRegionNot(String nombre, Integer id);
}
