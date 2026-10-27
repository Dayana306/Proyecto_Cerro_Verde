package pe.smcv.ptar.operacion.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.operacion.entity.EquipoMonitoreo;
import java.util.List;
public interface EquipoMonitoreoRepository extends JpaRepository<EquipoMonitoreo,Integer> {
 List<EquipoMonitoreo> findAllByOrderByCodigoAsc();
 boolean existsByCodigoIgnoreCase(String codigo);
 boolean existsByCodigoIgnoreCaseAndIdEquipoNot(String codigo,Integer idEquipo);
}
