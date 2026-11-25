package pe.smcv.ptar.ubicacion.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.ubicacion.dto.Mapa2dDto;
import pe.smcv.ptar.ubicacion.dto.MapaVisorResponse;
import pe.smcv.ptar.ubicacion.entity.Mapa2d;
import pe.smcv.ptar.ubicacion.repository.Mapa2dRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mapas base y datos del visor 2D (RF-08). */
@Service
public class Mapa2dService {

    private static final String ENTIDAD = "MAPA_2D";
    private static final Sort ORDEN = Sort.by(Sort.Direction.DESC, "fechaActualizacion");

    private final Mapa2dRepository repo;
    private final ZonaService zonaService;
    private final PuntoMonitoreoService puntoService;
    private final AlertaRepository alertaRepository;
    private final AuditoriaService auditoria;

    public Mapa2dService(Mapa2dRepository repo, ZonaService zonaService, PuntoMonitoreoService puntoService,
                         AlertaRepository alertaRepository, AuditoriaService auditoria) {
        this.repo = repo;
        this.zonaService = zonaService;
        this.puntoService = puntoService;
        this.alertaRepository = alertaRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Mapa2dDto.Response> listar(Estado estado) {
        List<Mapa2d> datos = estado == null ? repo.findAll(ORDEN) : repo.findByEstado(estado, ORDEN);
        return datos.stream().map(Mapa2dDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public Mapa2dDto.Response obtener(Integer id) {
        return Mapa2dDto.Response.desde(buscar(id));
    }

    @Transactional
    public Mapa2dDto.Response crear(Mapa2dDto.Request req) {
        Mapa2d m = new Mapa2d();
        aplicar(m, req);
        Mapa2dDto.Response r = Mapa2dDto.Response.desde(repo.save(m));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idMapa(), "Mapa creado: " + r.nombreMapa(), null, r);
        return r;
    }

    @Transactional
    public Mapa2dDto.Response actualizar(Integer id, Mapa2dDto.Request req) {
        Mapa2d m = buscar(id);
        Mapa2dDto.Response antes = Mapa2dDto.Response.desde(m);
        aplicar(m, req);
        Mapa2dDto.Response r = Mapa2dDto.Response.desde(repo.saveAndFlush(m));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Mapa modificado: " + r.nombreMapa(), antes, r);
        return r;
    }

    @Transactional
    public Mapa2dDto.Response cambiarEstado(Integer id, Estado estado) {
        Mapa2d m = buscar(id);
        m.setEstado(estado);
        Mapa2dDto.Response r = Mapa2dDto.Response.desde(repo.saveAndFlush(m));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Mapa " + m.getNombreMapa() + " -> " + estado, null, null);
        return r;
    }

    /**
     * Mapa base activo más reciente + zonas activas + puntos activos con su cantidad de alertas
     * abiertas y el nivel más alto (para colorear: sin alertas = verde, MEDIA = ámbar, ALTA = rojo).
     */
    @Transactional(readOnly = true)
    public MapaVisorResponse visor() {
        Mapa2dDto.Response base = repo.findFirstByEstadoOrderByFechaActualizacionDesc(Estado.ACTIVO)
                .map(Mapa2dDto.Response::desde)
                .orElse(null);

        Map<Integer, long[]> conteo = new HashMap<>(); // idPunto -> [total, altas]
        for (Object[] fila : alertaRepository.contarAbiertasPorPuntoYNivel()) {
            Integer idPunto = (Integer) fila[0];
            String nivel = String.valueOf(fila[1]);
            long cantidad = ((Number) fila[2]).longValue();
            long[] c = conteo.computeIfAbsent(idPunto, k -> new long[2]);
            c[0] += cantidad;
            if ("ALTA".equals(nivel)) {
                c[1] += cantidad;
            }
        }

        List<MapaVisorResponse.PuntoVisor> puntos = puntoService.listar(null, null, Estado.ACTIVO).stream()
                .map(p -> {
                    long[] c = conteo.getOrDefault(p.idPunto(), new long[2]);
                    String nivel = c[0] == 0 ? null : (c[1] > 0 ? "ALTA" : "MEDIA");
                    return new MapaVisorResponse.PuntoVisor(p, c[0], nivel);
                })
                .toList();

        return new MapaVisorResponse(base, zonaService.listar(null, Estado.ACTIVO), puntos);
    }

    private void aplicar(Mapa2d m, Mapa2dDto.Request req) {
        m.setNombreMapa(req.nombreMapa().trim());
        m.setDescripcion(req.descripcion());
        m.setImagenRuta(req.imagenRuta().trim());
    }

    private Mapa2d buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Mapa no encontrado: " + id));
    }
}
