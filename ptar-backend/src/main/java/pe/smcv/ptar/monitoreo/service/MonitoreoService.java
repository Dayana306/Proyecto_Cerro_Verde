package pe.smcv.ptar.monitoreo.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smcv.ptar.alerta.dto.AlertaResponse;
import pe.smcv.ptar.alerta.entity.Alerta;
import pe.smcv.ptar.alerta.entity.EstadoAlerta;
import pe.smcv.ptar.alerta.entity.NivelAlerta;
import pe.smcv.ptar.alerta.repository.AlertaRepository;
import pe.smcv.ptar.alerta.service.AlertaService;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.dto.PageResponse;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.Fechas;
import pe.smcv.ptar.monitoreo.dto.MonitoreoRequest;
import pe.smcv.ptar.monitoreo.dto.MonitoreoResponse;
import pe.smcv.ptar.monitoreo.entity.LecturaParametro;
import pe.smcv.ptar.monitoreo.entity.Monitoreo;
import pe.smcv.ptar.monitoreo.repository.MonitoreoRepository;
import pe.smcv.ptar.parametro.entity.Parametro;
import pe.smcv.ptar.parametro.service.ParametroService;
import pe.smcv.ptar.ubicacion.service.PuntoMonitoreoService;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Registro de mediciones (RF-02, RF-03, CU02).
 * Flujo: validar -> guardar monitoreo y lecturas -> evaluar umbrales -> generar alertas, todo en UNA transacción.
 */
@Service
public class MonitoreoService {

    private static final String ENTIDAD = "MONITOREO";
    /** Tolerancia por diferencia de relojes al validar que la hora no sea futura. */
    private static final long MINUTOS_TOLERANCIA_FUTURO = 10;

    private final MonitoreoRepository repo;
    private final PuntoMonitoreoService puntoService;
    private final ParametroService parametroService;
    private final UsuarioRepository usuarioRepository;
    private final AlertaService alertaService;
    private final AlertaRepository alertaRepository;
    private final AuditoriaService auditoria;

    public MonitoreoService(MonitoreoRepository repo, PuntoMonitoreoService puntoService,
                            ParametroService parametroService, UsuarioRepository usuarioRepository,
                            AlertaService alertaService, AlertaRepository alertaRepository,
                            AuditoriaService auditoria) {
        this.repo = repo;
        this.puntoService = puntoService;
        this.parametroService = parametroService;
        this.usuarioRepository = usuarioRepository;
        this.alertaService = alertaService;
        this.alertaRepository = alertaRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<MonitoreoResponse> buscar(Integer idPunto, Estado estado, LocalDate desde, LocalDate hasta,
                                                  Pageable pageable) {
        return PageResponse.from(repo.buscar(idPunto, estado, Fechas.inicio(desde), Fechas.fin(hasta), pageable),
                MonitoreoResponse::desde);
    }

    @Transactional(readOnly = true)
    public MonitoreoResponse obtener(Integer id) {
        return detalle(buscarCompleto(id));
    }

    /** Registro normal desde la API. */
    @Transactional
    public MonitoreoResponse registrar(MonitoreoRequest req, Integer idUsuario) {
        if (req.horaMuestreo().isAfter(LocalDateTime.now().plusMinutes(MINUTOS_TOLERANCIA_FUTURO))) {
            throw new ReglaNegocioException("La hora de muestreo no puede ser futura");
        }
        Monitoreo m = crearMonitoreo(req, idUsuario, null);
        MonitoreoResponse r = detalle(m);
        auditoria.registrar(AccionAuditoria.CREAR, ENTIDAD, m.getIdMonitoreo(),
                "Monitoreo registrado en " + r.punto() + " (" + r.lecturas().size() + " lecturas, "
                        + r.alertas().size() + " alertas)", null, r);
        return r;
    }

    /**
     * Registro de datos históricos (carga de demostración): la alerta toma la hora del muestreo
     * y no se registra en la bitácora.
     */
    @Transactional
    public void registrarHistorico(MonitoreoRequest req, Integer idUsuario) {
        crearMonitoreo(req, idUsuario, req.horaMuestreo());
    }

    /**
     * Corrección de un monitoreo. Las lecturas de parámetros que ya existían se actualizan en el mismo
     * registro; las nuevas se agregan; las que no vienen en la solicitud se desactivan.
     * Se re-evalúan las alertas de los valores que cambiaron.
     */
    @Transactional
    public MonitoreoResponse actualizar(Integer id, MonitoreoRequest req, Integer idUsuario) {
        Monitoreo m = buscarCompleto(id);
        if (m.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("No se puede editar un monitoreo anulado");
        }
        if (req.horaMuestreo().isAfter(LocalDateTime.now().plusMinutes(MINUTOS_TOLERANCIA_FUTURO))) {
            throw new ReglaNegocioException("La hora de muestreo no puede ser futura");
        }
        MonitoreoResponse antes = MonitoreoResponse.desde(m);
        Map<Integer, Parametro> parametros = validarLecturas(req.lecturas());

        m.setPunto(puntoService.buscarActivo(req.idPunto()));
        m.setTipoMuestreo(req.tipoMuestreo());
        m.setHoraMuestreo(req.horaMuestreo());
        m.setObservacion(limpiar(req.observacion()));

        Map<Integer, LecturaParametro> existentes = new HashMap<>();
        m.getLecturas().forEach(l -> existentes.put(l.getParametro().getIdParametro(), l));

        List<LecturaParametro> aEvaluar = new ArrayList<>();
        Map<LecturaParametro, BigDecimal> corregidas = new HashMap<>();
        for (MonitoreoRequest.LecturaRequest lr : req.lecturas()) {
            LecturaParametro l = existentes.remove(lr.idParametro());
            if (l == null) {
                l = new LecturaParametro();
                l.setParametro(parametros.get(lr.idParametro()));
                l.setValor(lr.valor());
                m.agregarLectura(l);
                aEvaluar.add(l);
            } else {
                boolean cambio = l.getValor().compareTo(lr.valor()) != 0 || l.getEstado() != Estado.ACTIVO;
                if (cambio) {
                    if (l.getEstado() == Estado.ACTIVO) {
                        corregidas.put(l, l.getValor());
                    }
                    l.setValor(lr.valor());
                    l.setEstado(Estado.ACTIVO);
                    aEvaluar.add(l);
                }
            }
        }
        existentes.values().forEach(l -> l.setEstado(Estado.INACTIVO)); // parámetros quitados

        repo.saveAndFlush(m);
        corregidas.forEach((l, anterior) -> alertaService.registrarCorreccion(l, anterior, idUsuario));
        aEvaluar.forEach(l -> alertaService.evaluar(l, null));

        MonitoreoResponse despues = detalle(m);
        auditoria.registrar(AccionAuditoria.MODIFICAR, ENTIDAD, id, "Monitoreo corregido en " + despues.punto(),
                antes, despues);
        return despues;
    }

    /** Anular (INACTIVO) cierra sus alertas abiertas. Reactivar no las reabre. */
    @Transactional
    public MonitoreoResponse cambiarEstado(Integer id, Estado estado, Integer idUsuario) {
        Monitoreo m = buscarCompleto(id);
        if (m.getEstado() == estado) {
            return detalle(m);
        }
        m.setEstado(estado);
        repo.save(m);
        int cerradas = 0;
        if (estado == Estado.INACTIVO) {
            cerradas = alertaService.cerrarPorAnulacion(id, idUsuario);
        }
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, id,
                "Monitoreo #" + id + " -> " + estado + (cerradas > 0 ? " (" + cerradas + " alertas cerradas)" : ""),
                null, null);
        return detalle(m);
    }

    @Transactional(readOnly = true)
    public Monitoreo buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Monitoreo no encontrado: " + id));
    }

    // ------------------------------------------------------------------

    private Monitoreo crearMonitoreo(MonitoreoRequest req, Integer idUsuario, LocalDateTime fechaDeteccion) {
        Map<Integer, Parametro> parametros = validarLecturas(req.lecturas());

        Monitoreo m = new Monitoreo();
        m.setPunto(puntoService.buscarActivo(req.idPunto()));
        m.setTipoMuestreo(req.tipoMuestreo());
        m.setHoraMuestreo(req.horaMuestreo());
        m.setObservacion(limpiar(req.observacion()));
        m.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        for (MonitoreoRequest.LecturaRequest lr : req.lecturas()) {
            LecturaParametro l = new LecturaParametro();
            l.setParametro(parametros.get(lr.idParametro()));
            l.setValor(lr.valor());
            m.agregarLectura(l);
        }
        repo.saveAndFlush(m); // asigna ids a las lecturas antes de crear alertas

        for (LecturaParametro l : m.getLecturas()) {
            alertaService.evaluar(l, fechaDeteccion);
        }
        return m;
    }

    /**
     * RF-03: parámetros activos, sin repetir, y valores dentro del rango físicamente válido.
     * Reúne todos los errores para mostrarlos juntos en el formulario.
     */
    private Map<Integer, Parametro> validarLecturas(List<MonitoreoRequest.LecturaRequest> lecturas) {
        Map<Integer, Parametro> parametros = new HashMap<>();
        Map<String, String> errores = new LinkedHashMap<>();
        for (int i = 0; i < lecturas.size(); i++) {
            MonitoreoRequest.LecturaRequest lr = lecturas.get(i);
            if (parametros.containsKey(lr.idParametro())) {
                errores.put("lecturas[" + i + "].idParametro", "Parámetro repetido");
                continue;
            }
            Parametro p = parametroService.buscarActivo(lr.idParametro());
            parametros.put(lr.idParametro(), p);
            if (!p.esValorValido(lr.valor())) {
                errores.put("lecturas[" + i + "].valor", String.format("%s fuera del rango válido [%s - %s] %s",
                        p.getNombreParametro(), p.getValorMinValido().stripTrailingZeros().toPlainString(),
                        p.getValorMaxValido().stripTrailingZeros().toPlainString(), p.getUnidadMedida()));
            }
        }
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException("Hay valores inválidos en las lecturas. Verifique los datos ingresados.",
                    errores);
        }
        return parametros;
    }

    private MonitoreoResponse detalle(Monitoreo m) {
        List<Alerta> alertas = m.getIdMonitoreo() == null ? List.of() : alertaRepository.deMonitoreo(m.getIdMonitoreo());
        Map<Long, NivelAlerta> nivelPorLectura = new HashMap<>();
        for (Alerta a : alertas) {
            if (a.getEstado() != EstadoAlerta.CERRADA) {
                nivelPorLectura.merge(a.getLectura().getIdLectura(), a.getNivel(),
                        (x, y) -> x == NivelAlerta.ALTA || y == NivelAlerta.ALTA ? NivelAlerta.ALTA : NivelAlerta.MEDIA);
            }
        }
        return MonitoreoResponse.desde(m, nivelPorLectura, alertas.stream().map(AlertaResponse::desde).toList());
    }

    private Monitoreo buscarCompleto(Integer id) {
        return repo.buscarCompleto(id).orElseThrow(() -> new NotFoundException("Monitoreo no encontrado: " + id));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
