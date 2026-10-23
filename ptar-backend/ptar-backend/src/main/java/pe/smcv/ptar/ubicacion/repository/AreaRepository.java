package pe.smcv.ptar.ubicacion.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Area;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    List<Area> findByEstado(Estado estado, Sort sort);
    Optional<Area> findByNombreAreaIgnoreCase(String nombre);
    boolean existsByNombreAreaIgnoreCase(String nombre);
    boolean existsByNombreAreaIgnoreCaseAndIdAreaNot(String nombre, Integer id);
}
