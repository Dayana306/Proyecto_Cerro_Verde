package pe.smcv.ptar.alerta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smcv.ptar.alerta.entity.Seguimiento;

import java.util.List;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Integer> {

    @Query("""
            select s from Seguimiento s join fetch s.usuario
            where s.alerta.idAlerta = :idAlerta
            order by s.fecha, s.idSeguimiento
            """)
    List<Seguimiento> deAlerta(@Param("idAlerta") Integer idAlerta);
}
