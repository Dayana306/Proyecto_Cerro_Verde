package pe.smcv.ptar.operacion.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.operacion.entity.PersonalOperativo;
import java.util.List;
public interface PersonalOperativoRepository extends JpaRepository<PersonalOperativo,Integer> {
 List<PersonalOperativo> findAllByOrderByApellidosAscNombresAsc();
 boolean existsByCorreoIgnoreCase(String correo);
}
