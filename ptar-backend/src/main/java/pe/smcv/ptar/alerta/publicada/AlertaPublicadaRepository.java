package pe.smcv.ptar.alerta.publicada;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertaPublicadaRepository extends JpaRepository<AlertaPublicada, Integer> {
    @EntityGraph(attributePaths = "creador")
    List<AlertaPublicada> findAllByOrderByFechaCreacionDesc();
}
