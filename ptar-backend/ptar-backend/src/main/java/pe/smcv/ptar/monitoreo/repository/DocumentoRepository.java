package pe.smcv.ptar.monitoreo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.monitoreo.entity.Documento;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    @Query("""
            select d from Documento d join fetch d.usuario
            where d.monitoreo.idMonitoreo = :idMonitoreo
              and (:incluirInactivos = true or d.estado = pe.smcv.ptar.common.enums.Estado.ACTIVO)
            order by d.fechaSubida desc
            """)
    List<Documento> deMonitoreo(@Param("idMonitoreo") Integer idMonitoreo,
                                @Param("incluirInactivos") boolean incluirInactivos);
}
