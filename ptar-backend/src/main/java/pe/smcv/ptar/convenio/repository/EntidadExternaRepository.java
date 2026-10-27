package pe.smcv.ptar.convenio.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.convenio.entity.EntidadExterna;

import java.util.List;
import java.util.Optional;

public interface EntidadExternaRepository extends JpaRepository<EntidadExterna, Integer> {
    List<EntidadExterna> findByEstado(Estado estado, Sort sort);
    Optional<EntidadExterna> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdEntidadNot(String nombre, Integer id);
}
