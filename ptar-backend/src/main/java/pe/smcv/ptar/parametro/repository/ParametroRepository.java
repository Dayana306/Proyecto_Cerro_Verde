package pe.smcv.ptar.parametro.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.parametro.entity.Parametro;

import java.util.List;
import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Integer> {
    List<Parametro> findByEstado(Estado estado, Sort sort);
    Optional<Parametro> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdParametroNot(String codigo, Integer id);
}
