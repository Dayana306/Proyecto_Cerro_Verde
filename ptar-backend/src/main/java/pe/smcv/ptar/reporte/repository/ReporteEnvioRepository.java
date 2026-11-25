package pe.smcv.ptar.reporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.reporte.entity.ReporteEnvio;

import java.util.List;

public interface ReporteEnvioRepository extends JpaRepository<ReporteEnvio, Integer> {

    @Query("""
            select e from ReporteEnvio e join fetch e.entidad join fetch e.usuario
            where e.reporte.idReporte = :idReporte
            order by e.fechaEnvio desc
            """)
    List<ReporteEnvio> deReporte(@Param("idReporte") Integer idReporte);
}
