package pe.smcv.ptar.operacion.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.operacion.entity.IncidenciaUsuario;
import java.util.List;
public interface IncidenciaUsuarioRepository extends JpaRepository<IncidenciaUsuario,Long> {
 List<IncidenciaUsuario> findAllByOrderByFechaReporteDesc();
 List<IncidenciaUsuario> findByIdUsuarioReportaOrderByFechaReporteDesc(Integer idUsuario);
}
