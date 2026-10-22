package pe.smcv.ptar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.common.enums.RolNombre;
import pe.smcv.ptar.usuario.entity.Rol;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.RolRepository;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

//desarrollador peude crear un administrador inicial si la tabla usuario esta vacía

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public DevDataInitializer(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.dev-admin.correo}") String correo,
                              @Value("${app.dev-admin.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Rol rolAdmin = rolRepository.findByNombre(RolNombre.ADMINISTRADOR.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol ADMINISTRADOR: revisar V2__datos_semilla.sql"));

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("Sistema");
        admin.setCorreo(correo);
        admin.setUsuarioLogin("admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(rolAdmin);
        usuarioRepository.save(admin);

        log.warn("Usuario administrador de DESARROLLO creado: {} (cambiar la contraseña)", correo);
    }
}
