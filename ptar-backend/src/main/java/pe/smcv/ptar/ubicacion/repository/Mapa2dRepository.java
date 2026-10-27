package pe.smcv.ptar.ubicacion.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.ubicacion.entity.Mapa2d;

import java.util.List;
import java.util.Optional;

public interface Mapa2dRepository extends JpaRepository<Mapa2d, Integer> {
    List<Mapa2d> findByEstado(Estado estado, Sort sort);
    Optional<Mapa2d> findFirstByEstadoOrderByFechaActualizacionDesc(Estado estado);
}
