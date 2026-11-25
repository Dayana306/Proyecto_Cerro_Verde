package pe.smcv.ptar.reporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.reporte.entity.ReporteDetalle;

import java.util.List;

public interface ReporteDetalleRepository extends JpaRepository<ReporteDetalle, Integer> {

    @Query("""
            select d from ReporteDetalle d join fetch d.parametro join fetch d.punto p
            where d.reporte.idReporte = :idReporte
            order by p.nombrePunto, d.parametro.nombreParametro
            """)
    List<ReporteDetalle> deReporte(@Param("idReporte") Integer idReporte);
}
