package pe.smcv.ptar.reporte.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.reporte.entity.EstadoReporte;
import pe.smcv.ptar.reporte.entity.Reporte;
import pe.smcv.ptar.reporte.entity.TipoReporte;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    String FILTRO = """
            where (:tipo is null or r.tipoReporte = :tipo)
              and (:estado is null or r.estado = :estado)
            """;

    @Query(value = "select r from Reporte r join fetch r.usuario left join fetch r.punto " + FILTRO,
            countQuery = "select count(r) from Reporte r " + FILTRO)
    Page<Reporte> buscar(@Param("tipo") TipoReporte tipo, @Param("estado") EstadoReporte estado, Pageable pageable);
}
