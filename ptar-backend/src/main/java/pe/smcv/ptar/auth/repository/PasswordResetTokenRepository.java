package pe.smcv.ptar.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.smcv.ptar.auth.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsadoFalse(String tokenHash);
    List<PasswordResetToken> findByUsuario_IdUsuarioAndUsadoFalse(Integer idUsuario);
}
