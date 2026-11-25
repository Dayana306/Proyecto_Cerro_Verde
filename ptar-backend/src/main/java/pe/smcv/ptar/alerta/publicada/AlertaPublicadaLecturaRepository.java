package pe.smcv.ptar.alerta.publicada;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlertaPublicadaLecturaRepository extends JpaRepository<AlertaPublicadaLectura, Integer> {
    Optional<AlertaPublicadaLectura> findByAlerta_IdAlertaPublicadaAndUsuario_IdUsuario(Integer idAlerta, Integer idUsuario);
}
