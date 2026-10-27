package pe.smcv.ptar.operacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.operacion.entity.EquipoMonitoreo;
import pe.smcv.ptar.operacion.entity.MantenimientoProgramado;
import pe.smcv.ptar.operacion.entity.PersonalOperativo;
import pe.smcv.ptar.operacion.repository.EquipoMonitoreoRepository;
import pe.smcv.ptar.operacion.repository.MantenimientoProgramadoRepository;
import pe.smcv.ptar.operacion.repository.PersonalOperativoRepository;
import pe.smcv.ptar.ubicacion.entity.PuntoMonitoreo;
import pe.smcv.ptar.ubicacion.repository.PuntoMonitoreoRepository;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Datos de operación ficticios, únicamente si el perfil dev y app.demo.enabled=true lo solicitan. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@Order(20)
public class OperacionDemoDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(OperacionDemoDataInitializer.class);
    private final PersonalOperativoRepository personalRepo;
    private final EquipoMonitoreoRepository equipoRepo;
    private final MantenimientoProgramadoRepository mantenimientoRepo;
    private final PuntoMonitoreoRepository puntoRepo;
    private final UsuarioRepository usuarioRepo;

    public OperacionDemoDataInitializer(PersonalOperativoRepository personalRepo, EquipoMonitoreoRepository equipoRepo,
                                        MantenimientoProgramadoRepository mantenimientoRepo,
                                        PuntoMonitoreoRepository puntoRepo, UsuarioRepository usuarioRepo) {
        this.personalRepo = personalRepo; this.equipoRepo = equipoRepo; this.mantenimientoRepo = mantenimientoRepo;
        this.puntoRepo = puntoRepo; this.usuarioRepo = usuarioRepo;
    }

    @Override public void run(String... args) {
        if (personalRepo.count() > 0 || equipoRepo.count() > 0) return;
        List<PersonalOperativo> staff = List.of(
            persona("Carlos", "Quispe", "Supervisor de turno", "Operaciones PTAR", "07:00–15:00", "carlos.quispe.demo@smcv.local"),
            persona("Rosa", "Mamani", "Técnica de laboratorio", "Laboratorio PTAR", "07:00–15:00", "rosa.mamani.demo@smcv.local"),
            persona("Luis", "Condori", "Técnico de instrumentación", "Mantenimiento", "15:00–23:00", "luis.condori.demo@smcv.local"),
            persona("Ana", "Paredes", "Operadora de monitoreo", "Control de procesos", "23:00–07:00", "ana.paredes.demo@smcv.local"),
            persona("Miguel", "Salas", "Técnico electromecánico", "Mantenimiento", "08:00–17:00", "miguel.salas.demo@smcv.local")
        );
        personalRepo.saveAll(staff);
        List<PersonalOperativo> saved = personalRepo.findAllByOrderByApellidosAscNombresAsc();
        Integer pointId = puntoRepo.findAll().stream().findFirst().map(PuntoMonitoreo::getIdPunto).orElse(null);
        EquipoMonitoreo ph = equipo("PH-001", "Medidor de pH en línea", "Medidor de pH", "AUTOMATICO", "SIN_CONEXION", pointId, saved.get(0).getIdPersonal());
        ph.setObservaciones("DEMO: la conexión al instrumento físico no está configurada."); ph.setProximaRevision(LocalDate.now().plusDays(14));
        EquipoMonitoreo portable = equipo("PH-002", "Medidor portátil de pH", "Medidor de pH", "MANUAL", "NO_APLICA", pointId, saved.get(1).getIdPersonal());
        portable.setProximaRevision(LocalDate.now().plusDays(30));
        EquipoMonitoreo turbidity = equipo("TB-001", "Turbidímetro de salida", "Turbidímetro", "AUTOMATICO", "SIN_CONEXION", pointId, saved.get(2).getIdPersonal());
        turbidity.setProximaRevision(LocalDate.now().plusDays(10));
        EquipoMonitoreo flow = equipo("FL-001", "Caudalímetro de canal", "Caudalímetro", "SEMIAUTOMATICO", "SIN_CONEXION", pointId, saved.get(3).getIdPersonal());
        flow.setProximaRevision(LocalDate.now().plusDays(7));
        equipoRepo.saveAll(List.of(ph, portable, turbidity, flow));
        Usuario admin = usuarioRepo.findByCorreoIgnoreCase("admin@smcv.local").orElse(null);
        if (admin != null && mantenimientoRepo.count() == 0) {
            MantenimientoProgramado m = new MantenimientoProgramado(); m.setIdEquipo(ph.getIdEquipo()); m.setIdPersonalAsignado(saved.get(2).getIdPersonal());
            m.setFechaHoraProgramada(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0));
            m.setTipo("Preventivo"); m.setDescripcion("DEMO: revisar la carcasa, la calibración y la disponibilidad de conexión del medidor.");
            m.setEstado("PROGRAMADO"); m.setIdUsuarioCreador(admin.getIdUsuario()); mantenimientoRepo.save(m);
        }
        log.warn("Se cargó personal, equipos y mantenimiento FICTICIOS de demostración. No representan empleados ni conexiones reales.");
    }

    private PersonalOperativo persona(String n, String a, String cargo, String area, String turno, String correo) {
        PersonalOperativo p = new PersonalOperativo(); p.setNombres(n); p.setApellidos(a); p.setCargo(cargo); p.setArea(area);
        p.setTurno(turno); p.setCorreo(correo); p.setEstado("ACTIVO"); p.setDemo(true); return p;
    }
    private EquipoMonitoreo equipo(String codigo, String nombre, String tipo, String modo, String conexion, Integer punto, Integer responsable) {
        EquipoMonitoreo e = new EquipoMonitoreo(); e.setCodigo(codigo); e.setNombre(nombre); e.setTipoEquipo(tipo); e.setModoLectura(modo);
        e.setEstado("ACTIVO"); e.setEstadoConexion(conexion); e.setIdPunto(punto); e.setIdPersonalResponsable(responsable); e.setDemo(true); return e;
    }
}
