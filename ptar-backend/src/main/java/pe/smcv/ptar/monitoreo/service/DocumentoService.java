package pe.smcv.ptar.monitoreo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.enums.Estado;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.common.exception.ReglaNegocioException;
import pe.smcv.ptar.common.util.AlmacenamientoService;
import pe.smcv.ptar.monitoreo.dto.DocumentoResponse;
import pe.smcv.ptar.monitoreo.entity.Documento;
import pe.smcv.ptar.monitoreo.entity.Monitoreo;
import pe.smcv.ptar.monitoreo.repository.DocumentoRepository;
import pe.smcv.ptar.usuario.repository.UsuarioRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Archivos de respaldo de un monitoreo (actas, fotos, resultados de laboratorio). */
@Service
public class DocumentoService {

    private static final String ENTIDAD = "DOCUMENTO";
    private static final Set<String> EXTENSIONES =
            Set.of("pdf", "png", "jpg", "jpeg", "xlsx", "xls", "docx", "doc", "csv", "txt");

    private final DocumentoRepository repo;
    private final MonitoreoService monitoreoService;
    private final UsuarioRepository usuarioRepository;
    private final AlmacenamientoService almacenamiento;
    private final AuditoriaService auditoria;

    public DocumentoService(DocumentoRepository repo, MonitoreoService monitoreoService,
                            UsuarioRepository usuarioRepository, AlmacenamientoService almacenamiento,
                            AuditoriaService auditoria) {
        this.repo = repo;
        this.monitoreoService = monitoreoService;
        this.usuarioRepository = usuarioRepository;
        this.almacenamiento = almacenamiento;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> deMonitoreo(Integer idMonitoreo, boolean incluirInactivos) {
        monitoreoService.buscar(idMonitoreo);
        return repo.deMonitoreo(idMonitoreo, incluirInactivos).stream().map(DocumentoResponse::desde).toList();
    }

    @Transactional
    public DocumentoResponse subir(Integer idMonitoreo, MultipartFile archivo, String descripcion, Integer idUsuario) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe adjuntar un archivo");
        }
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONES.contains(extension)) {
            throw new ReglaNegocioException("Tipo de archivo no permitido. Permitidos: " + String.join(", ", EXTENSIONES));
        }
        Monitoreo m = monitoreoService.buscar(idMonitoreo);

        String ruta;
        try {
            ruta = almacenamiento.guardar("documentos", nombre, archivo.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo subido", e);
        }
        Documento d = new Documento();
        d.setMonitoreo(m);
        d.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        d.setNombreOriginal(nombre.length() > 255 ? nombre.substring(0, 255) : nombre);
        d.setRutaArchivo(ruta);
        d.setTipoContenido(archivo.getContentType());
        d.setTamanoBytes(archivo.getSize());
        d.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        DocumentoResponse r = DocumentoResponse.desde(repo.save(d));
        auditoria.registrar(AccionAuditoria.SUBIR_ARCHIVO, ENTIDAD, r.idDocumento(),
                "Documento '" + r.nombreOriginal() + "' en monitoreo #" + idMonitoreo, null, null);
        return r;
    }

    /** Devuelve [documento, contenido]. */
    @Transactional(readOnly = true)
    public Archivo descargar(Integer idDocumento) {
        Documento d = buscar(idDocumento);
        return new Archivo(d.getNombreOriginal(), d.getTipoContenido(), almacenamiento.leer(d.getRutaArchivo()));
    }

    @Transactional
    public DocumentoResponse cambiarEstado(Integer idDocumento, Estado estado) {
        Documento d = buscar(idDocumento);
        d.setEstado(estado);
        DocumentoResponse r = DocumentoResponse.desde(repo.save(d));
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, ENTIDAD, idDocumento,
                "Documento '" + d.getNombreOriginal() + "' -> " + estado, null, null);
        return r;
    }

    private Documento buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
    }

    public record Archivo(String nombre, String tipoContenido, byte[] contenido) {}
}
