package pe.smcv.ptar.ubicacion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.ubicacion.dto.ZonaDto;
import pe.smcv.ptar.ubicacion.entity.Zona;
import pe.smcv.ptar.ubicacion.repository.ZonaRepository;

import java.util.List;
import java.util.Set;

@Service
public class ZonaService {

    private static final String ENTIDAD = "ZONA";
    private static final Set<String> TIPOS_GEOJSON =
            Set.of("Polygon", "MultiPolygon", "Feature", "FeatureCollection");

    private final ZonaRepository repo;
    private final RegionService regionService;
    private final ObjectMapper objectMapper;
    private final AuditoriaService auditoria;

    public ZonaService(ZonaRepository repo, RegionService regionService, ObjectMapper objectMapper,
                       AuditoriaService auditoria) {
        this.repo = repo;
        this.regionService = regionService;
        this.objectMapper = objectMapper;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ZonaDto.Response> listar(Integer idRegion, Estado estado) {
        return repo.buscar(idRegion, estado).stream().map(ZonaDto.Response::desde).toList();
    }

    @Transactional(readOnly = true)
    public ZonaDto.Response obtener(Integer id) {
        return ZonaDto.Response.desde(buscar(id));
    }

    @Transactional
    public ZonaDto.Response crear(ZonaDto.Request req) {
        String nombre = req.nombreZona().trim();
        if (repo.existsByNombreZonaIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una zona con ese nombre");
        }
        Zona z = new Zona();
        aplicar(z, req, nombre);
        ZonaDto.Response r = ZonaDto.Response.desde(repo.save(z));
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, r.idZona(), "Zona creada: " + nombre, null, r);
        return r;
    }

    @Transactional
    public ZonaDto.Response actualizar(Integer id, ZonaDto.Request req) {
        Zona z = buscar(id);
        ZonaDto.Response antes = ZonaDto.Response.desde(z);
        String nombre = req.nombreZona().trim();
        if (repo.existsByNombreZonaIgnoreCaseAndIdZonaNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe una zona con ese nombre");
        }
        aplicar(z, req, nombre);
        ZonaDto.Response r = ZonaDto.Response.desde(repo.save(z));
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Zona modificada: " + nombre, antes, r);
        return r;
    }

    @Transactional
    public ZonaDto.Response cambiarEstado(Integer id, Estado estado) {
        Zona z = buscar(id);
        z.setEstado(estado);
        ZonaDto.Response r = ZonaDto.Response.desde(repo.save(z));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Zona " + z.getNombreZona() + " -> " + estado, null, null);
        return r;
    }

    @Transactional(readOnly = true)
    public Zona buscarActiva(Integer id) {
        Zona z = buscar(id);
        if (z.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("La zona seleccionada está inactiva");
        }
        return z;
    }

    private void aplicar(Zona z, ZonaDto.Request req, String nombre) {
        z.setNombreZona(nombre);
        z.setDescripcion(req.descripcion());
        z.setTipoZona(req.tipoZona());
        z.setPoligonoGeojson(validarGeoJson(req.poligonoGeojson()));
        z.setRegion(regionService.buscarActiva(req.idRegion()));
    }

    /** Devuelve null si viene vacío; si no, exige un JSON con un "type" GeoJSON admitido. */
    private String validarGeoJson(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        JsonNode nodo;
        try {
            nodo = objectMapper.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new ReglaNegocioException("El polígono no es un JSON válido");
        }
        if (nodo == null || !TIPOS_GEOJSON.contains(nodo.path("type").asText(""))) {
            throw new ReglaNegocioException(
                    "El GeoJSON debe ser de tipo Polygon, MultiPolygon, Feature o FeatureCollection");
        }
        return texto.trim();
    }

    private Zona buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Zona no encontrada: " + id));
    }
}
