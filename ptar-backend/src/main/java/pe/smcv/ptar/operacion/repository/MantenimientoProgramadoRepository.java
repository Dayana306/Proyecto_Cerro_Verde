package pe.smcv.ptar.operacion.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.operacion.entity.MantenimientoProgramado;
import java.util.List;
public interface MantenimientoProgramadoRepository extends JpaRepository<MantenimientoProgramado,Long> {
 List<MantenimientoProgramado> findAllByOrderByFechaHoraProgramadaAsc();
}
