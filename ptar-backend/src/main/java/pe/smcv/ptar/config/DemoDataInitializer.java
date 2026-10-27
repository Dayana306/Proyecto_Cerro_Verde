package pe.smcv.ptar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.smcv.ptar.alerta.dto.SeguimientoRequest;
import pe.smcv.ptar.alerta.entity.Alerta;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.alerta.repository.NotificacionRepository;
import pe.smcv.ptar.alerta.service.AlertaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.enums.RolNombre;
import pe.smcv.ptar.convenio.entity.Convenio;
import pe.smcv.ptar.convenio.entity.TipoMeta;
import pe.smcv.ptar.convenio.repository.ConvenioRepository;
import pe.smcv.ptar.convenio.repository.EntidadExternaRepository;
import pe.smcv.ptar.monitoreo.dto.MonitoreoRequest;
import pe.smcv.ptar.monitoreo.entity.TipoMuestreo;
import pe.smcv.ptar.monitoreo.service.MonitoreoService;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.entity.Umbral;
import pe.smcv.ptar.parametro.repository.ParametroRepository;
import pe.smcv.ptar.parametro.repository.UmbralRepository;
import pe.smcv.ptar.ubicacion.entity.*;
import pe.smcv.ptar.ubicacion.repository.*;
import pe.smcv.ptar.usuario.entity.Usuario;
import pe.smcv.ptar.usuario.repository.RolRepository;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DATOS DE DEMOSTRACIÓN (solo perfil "dev" y app.demo.enabled=true).
 * Se cargan una sola vez: si ya existe algún punto de monitoreo, no hace nada.
 *
 * Crea: usuarios de cada rol, región/zonas/puntos de la PTAR, mapa base, umbrales de EJEMPLO
 * (no son límites normativos oficiales), convenio con SEDAPAR y ~45 días de mediciones con alertas.
 * Para volver a cargarlos: borrar la base (docker compose down -v) y arrancar de nuevo.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@Order(10)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    public static final String PASSWORD_DEMO = "Demo12345!";
    private static final int DIAS = 45;
    private static final int DIAS_RECIENTES = 3; // alertas de estos días quedan abiertas

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;
    private final ZonaRepository zonaRepository;
    private final PuntoMonitoreoRepository puntoRepository;
    private final Mapa2dRepository mapaRepository;
    private final ParametroRepository parametroRepository;
    private final UmbralRepository umbralRepository;
    private final EntidadExternaRepository entidadRepository;
    private final ConvenioRepository convenioRepository;
    private final MonitoreoService monitoreoService;
    private final AlertaRepository alertaRepository;
    private final AlertaService alertaService;
    private final NotificacionRepository notificacionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                               AreaRepository areaRepository, RegionRepository regionRepository,
                               ZonaRepository zonaRepository, PuntoMonitoreoRepository puntoRepository,
                               Mapa2dRepository mapaRepository, ParametroRepository parametroRepository,
                               UmbralRepository umbralRepository, EntidadExternaRepository entidadRepository,
                               ConvenioRepository convenioRepository, MonitoreoService monitoreoService,
                               AlertaRepository alertaRepository, AlertaService alertaService,
                               NotificacionRepository notificacionRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.regionRepository = regionRepository;
        this.zonaRepository = zonaRepository;
        this.puntoRepository = puntoRepository;
        this.mapaRepository = mapaRepository;
        this.parametroRepository = parametroRepository;
        this.umbralRepository = umbralRepository;
        this.entidadRepository = entidadRepository;
        this.convenioRepository = convenioRepository;
        this.monitoreoService = monitoreoService;
        this.alertaRepository = alertaRepository;
        this.alertaService = alertaService;
        this.notificacionRepository = notificacionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (puntoRepository.count() > 0) {
            return;
        }
        log.info("Cargando datos de demostración (perfil dev)...");

        Area gma = areaRepository.findByNombreAreaIgnoreCase("Gerencia de Medio Ambiente")
                .orElseGet(() -> area("Gerencia de Medio Ambiente", "Responsable del monitoreo hídrico de la PTAR"));
        Area operaciones = area("Operaciones PTAR", "Operación de la planta de tratamiento");

        Usuario admin = usuarioRepository.findByCorreoIgnoreCase("admin@smcv.local")
                .orElseThrow(() -> new IllegalStateException("Falta el administrador de V2__datos_iniciales.sql"));
        Usuario supervisor = usuario("Carlos", "Quispe", "supervisor@smcv.local", "supervisor", RolNombre.SUPERVISOR, operaciones);
        Usuario gerente = usuario("María", "Fernández", "gerente@smcv.local", "gerente", RolNombre.GERENTE, gma);
        usuario("Jorge", "Ramírez", "auditor@smcv.local", "auditor", RolNombre.AUDITOR, null);

        // ---- Ubicación (coordenadas referenciales de la zona de Uchumayo, Arequipa) ----
        Region arequipa = new Region();
        arequipa.setNombreRegion("Arequipa");
        arequipa.setDescripcion("Cuenca del río Chili");
        regionRepository.save(arequipa);

        Zona ptar = zona("PTAR La Enlozada", TipoZona.COMPONENTE, arequipa,
                poligono(-16.4285, -71.6185, -16.4225, -71.6105));
        Zona rio = zona("Río Chili - tramo aguas abajo", TipoZona.AREA_INFLUENCIA, arequipa,
                poligono(-16.4330, -71.6260, -16.4270, -71.6170));
        zona("Centro poblado Uchumayo", TipoZona.POBLADO, arequipa,
                poligono(-16.4290, -71.6720, -16.4180, -71.6580));

        PuntoMonitoreo ingreso = punto("Ingreso PTAR (afluente)", "Agua residual cruda que llega a la planta",
                -16.4248, -71.6170, ptar, operaciones);
        PuntoMonitoreo salida = punto("Salida PTAR (efluente)", "Agua tratada a la salida de la planta",
                -16.4262, -71.6128, ptar, operaciones);
        PuntoMonitoreo descarga = punto("Punto de descarga - Río Chili", "Vertimiento del efluente al río",
                -16.4296, -71.6205, rio, gma);

        Mapa2d mapa = new Mapa2d();
        mapa.setNombreMapa("Plano general PTAR La Enlozada");
        mapa.setDescripcion("Imagen de referencia. Reemplazar por el plano real.");
        mapa.setImagenRuta("/mapas/ptar-la-enlozada.png");
        mapaRepository.save(mapa);

        // ---- Umbrales de EJEMPLO ----
        Parametro ph = parametro("PH");
        Parametro caudal = parametro("CAUDAL");
        Parametro turbidez = parametro("TURBIDEZ");
        Parametro temperatura = parametro("TEMPERATURA");
        umbral(ph, null, "6.5", "8.5", "6.0", "9.0", admin);
        umbral(caudal, null, "1.2", null, "0.8", null, admin);
        umbral(temperatura, null, null, "35", null, "40", admin);
        // La turbidez del afluente es alta por naturaleza: solo se controla en salida y descarga.
        umbral(turbidez, salida, null, "50", null, "100", admin);
        umbral(turbidez, descarga, null, "50", null, "100", admin);

        Convenio convenio = new Convenio();
        convenio.setNombre("Convenio de uso de agua residual tratada SMCV - SEDAPAR");
        convenio.setEntidad(entidadRepository.findByNombreIgnoreCase("SEDAPAR")
                .orElseThrow(() -> new IllegalStateException("Falta SEDAPAR en V2__datos_iniciales.sql")));
        convenio.setPunto(salida);
        convenio.setFechaInicio(LocalDate.of(LocalDate.now().getYear() - 1, 1, 1));
        convenio.setCaudalMetaM3s(new BigDecimal("1.500"));
        convenio.setTipoMeta(TipoMeta.PROMEDIO_ANUAL);
        convenio.setObservacion("Datos de demostración");
        convenioRepository.save(convenio);

        // ---- Mediciones: dos muestreos diarios por punto ----
        Random rnd = new Random(42);
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        int total = 0;
        for (int d = DIAS; d >= 0; d--) {
            if (d == DIAS_RECIENTES - 1) {
                cerrarHistorico(gerente, hoy.minusDays(DIAS_RECIENTES - 1L).atStartOfDay());
            }
            LocalDate dia = hoy.minusDays(d);
            for (int hora : new int[]{8, 16}) {
                LocalDateTime momento = dia.atTime(hora, 0);
                if (momento.isAfter(ahora)) {
                    continue;
                }
                for (PuntoMonitoreo p : List.of(ingreso, salida, descarga)) {
                    monitoreoService.registrarHistorico(muestreo(p, momento, rnd, ingreso, salida,
                            ph, caudal, turbidez, temperatura), supervisor.getIdUsuario());
                    total++;
                }
            }
        }
        log.info("Datos de demostración cargados: {} monitoreos. Usuarios demo con contraseña {}", total, PASSWORD_DEMO);
    }

    /** Las alertas antiguas se cierran y sus notificaciones se marcan como leídas. */
    private void cerrarHistorico(Usuario gerente, LocalDateTime antesDe) {
        List<Alerta> antiguas = alertaRepository.findAll().stream()
                .filter(a -> a.getEstado() != EstadoAlerta.CERRADA && a.getFechaDeteccion().isBefore(antesDe))
                .toList();
        for (Alerta a : antiguas) {
            alertaService.agregarSeguimiento(a.getIdAlerta(), new SeguimientoRequest(
                    "Revisado con operaciones: valor puntual, se verificó la estabilidad del proceso.",
                    EstadoAlerta.CERRADA), gerente.getIdUsuario());
        }
        for (Usuario u : usuarioRepository.findAll()) {
            notificacionRepository.marcarTodasLeidas(u.getIdUsuario(), LocalDateTime.now());
        }
    }

    private MonitoreoRequest muestreo(PuntoMonitoreo p, LocalDateTime momento, Random rnd,
                                      PuntoMonitoreo ingreso, PuntoMonitoreo salida,
                                      Parametro ph, Parametro caudal, Parametro turbidez, Parametro temperatura) {
        boolean esIngreso = p.getIdPunto().equals(ingreso.getIdPunto());
        boolean esSalida = p.getIdPunto().equals(salida.getIdPunto());

        double vPh = 7.3 + rnd.nextGaussian() * 0.3;
        double vCaudal = (esIngreso ? 1.75 : esSalida ? 1.62 : 1.58) + rnd.nextGaussian() * 0.12;
        double vTurb = esIngreso ? 110 + rnd.nextGaussian() * 20 : 18 + rnd.nextGaussian() * 7;
        double vTemp = 21 + rnd.nextGaussian() * 1.5;

        // Eventos esporádicos para que existan alertas de ambos niveles.
        double evento = rnd.nextDouble();
        if (evento < 0.03) {
            vPh = rnd.nextBoolean() ? 8.7 + rnd.nextDouble() * 0.6 : 5.7 + rnd.nextDouble() * 0.6;
        } else if (evento < 0.05 && !esIngreso) {
            vTurb = 55 + rnd.nextDouble() * 60;
        } else if (evento < 0.065) {
            vCaudal = 0.7 + rnd.nextDouble() * 0.45;
        }

        List<MonitoreoRequest.LecturaRequest> lecturas = new ArrayList<>();
        lecturas.add(new MonitoreoRequest.LecturaRequest(ph.getIdParametro(), bd(vPh, 2)));
        lecturas.add(new MonitoreoRequest.LecturaRequest(caudal.getIdParametro(), bd(Math.max(0.1, vCaudal), 3)));
        lecturas.add(new MonitoreoRequest.LecturaRequest(turbidez.getIdParametro(), bd(Math.max(0.5, vTurb), 1)));
        lecturas.add(new MonitoreoRequest.LecturaRequest(temperatura.getIdParametro(), bd(vTemp, 1)));
        return new MonitoreoRequest(p.getIdPunto(), TipoMuestreo.MANUAL, momento, null, lecturas);
    }

    // ---------------- helpers ----------------

    private Area area(String nombre, String descripcion) {
        Area a = new Area();
        a.setNombreArea(nombre);
        a.setDescripcion(descripcion);
        return areaRepository.save(a);
    }

    private Usuario usuario(String nombre, String apellido, String correo, String login, RolNombre rol, Area area) {
        return usuarioRepository.findByCorreoIgnoreCase(correo).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setNombre(nombre);
            u.setApellido(apellido);
            u.setCorreo(correo);
            u.setUsuarioLogin(login);
            u.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
            u.setRol(rolRepository.findByNombre(rol.name()).orElseThrow());
            u.setIdArea(area == null ? null : area.getIdArea());
            return usuarioRepository.save(u);
        });
    }

    private Zona zona(String nombre, TipoZona tipo, Region region, String geojson) {
        Zona z = new Zona();
        z.setNombreZona(nombre);
        z.setTipoZona(tipo);
        z.setRegion(region);
        z.setPoligonoGeojson(geojson);
        return zonaRepository.save(z);
    }

    private PuntoMonitoreo punto(String nombre, String descripcion, double lat, double lon, Zona zona, Area area) {
        PuntoMonitoreo p = new PuntoMonitoreo();
        p.setNombrePunto(nombre);
        p.setDescripcion(descripcion);
        p.setLatitud(bd(lat, 6));
        p.setLongitud(bd(lon, 6));
        p.setZona(zona);
        p.setArea(area);
        return puntoRepository.save(p);
    }

    private Parametro parametro(String codigo) {
        return parametroRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + codigo + " en V2"));
    }

    private void umbral(Parametro p, PuntoMonitoreo punto, String limMin, String limMax, String critMin,
                        String critMax, Usuario admin) {
        Umbral u = new Umbral();
        u.setParametro(p);
        u.setPunto(punto);
        u.setLimiteMin(limMin == null ? null : new BigDecimal(limMin));
        u.setLimiteMax(limMax == null ? null : new BigDecimal(limMax));
        u.setCriticoMin(critMin == null ? null : new BigDecimal(critMin));
        u.setCriticoMax(critMax == null ? null : new BigDecimal(critMax));
        u.setUsuario(admin);
        umbralRepository.save(u);
    }

    /** Rectángulo GeoJSON (Polygon) a partir de dos esquinas. GeoJSON usa [longitud, latitud]. */
    private static String poligono(double lat1, double lon1, double lat2, double lon2) {
        return String.format(java.util.Locale.ROOT,
                "{\"type\":\"Polygon\",\"coordinates\":[[[%f,%f],[%f,%f],[%f,%f],[%f,%f],[%f,%f]]]}",
                lon1, lat1, lon2, lat1, lon2, lat2, lon1, lat2, lon1, lat1);
    }

    private static BigDecimal bd(double v, int escala) {
        return BigDecimal.valueOf(v).setScale(escala, RoundingMode.HALF_UP);
    }
}
