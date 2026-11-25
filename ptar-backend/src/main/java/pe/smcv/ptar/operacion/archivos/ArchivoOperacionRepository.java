package pe.smcv.ptar.operacion.archivos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArchivoOperacionRepository extends JpaRepository<ArchivoOperacion, Long> {
    List<ArchivoOperacion> findByIdIncidenciaOrderByFechaSubidaDesc(Long idIncidencia);
    List<ArchivoOperacion> findByPublicadoTrueAndEstadoOrderByFechaSubidaDesc(String estado);
    List<ArchivoOperacion> findByClaseOrderByFechaSubidaDesc(String clase);
    long countByIdIncidenciaAndClase(Long idIncidencia, String clase);
}
