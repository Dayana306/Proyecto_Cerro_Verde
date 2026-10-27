package pe.smcv.ptar.operacion.archivos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.smcv.ptar.auditoria.entity.AccionAuditoria;
import pe.smcv.ptar.auditoria.service.AuditoriaService;
import pe.smcv.ptar.common.util.AlmacenamientoService;
import pe.smcv.ptar.operacion.entity.IncidenciaUsuario;
import pe.smcv.ptar.operacion.repository.IncidenciaUsuarioRepository;
import pe.smcv.ptar.security.UsuarioPrincipal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ArchivoOperacionService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("pdf", "png", "jpg", "jpeg", "webp", "xlsx", "xls", "csv", "txt", "docx", "doc");
    private final ArchivoOperacionRepository archivoRepo;
    private final IncidenciaUsuarioRepository incidenciaRepo;
    private final AlmacenamientoService almacenamiento;
    private final AuditoriaService auditoria;

    public ArchivoOperacionService(ArchivoOperacionRepository archivoRepo, IncidenciaUsuarioRepository incidenciaRepo,
                                   AlmacenamientoService almacenamiento, AuditoriaService auditoria) {
        this.archivoRepo=archivoRepo; this.incidenciaRepo=incidenciaRepo; this.almacenamiento=almacenamiento; this.auditoria=auditoria;
    }

    @Transactional(readOnly = true)
    public List<ArchivoOperacionResponse> adjuntosDeIncidencia(Long id, UsuarioPrincipal actual) {
        IncidenciaUsuario i = buscarIncidencia(id); verificarPropiedadOAdmin(i, actual);
        return archivoRepo.findByIdIncidenciaOrderByFechaSubidaDesc(id).stream()
                .filter(a -> "ADJUNTO_REPORTE".equals(a.getClase()) && ("ADMINISTRADOR".equals(actual.getRol()) || a.getEstado().equals("ACTIVO")))
                .map(ArchivoOperacionResponse::desde).toList();
    }

    @Transactional
    public ArchivoOperacionResponse subirAdjunto(Long id, MultipartFile archivo, String descripcion, UsuarioPrincipal actual) {
        IncidenciaUsuario incidencia=buscarIncidencia(id); verificarPropiedadOAdmin(incidencia, actual);
        validarArchivo(archivo);
        if (archivoRepo.countByIdIncidenciaAndClase(id, "ADJUNTO_REPORTE") >= 5) throw badRequest("Cada observación admite un máximo de 5 archivos adjuntos.");
        ArchivoOperacion a=base(archivo, actual, "ADJUNTO_REPORTE"); a.setIdIncidencia(id); a.setPublicado(false);
        String tituloAdjunto = archivo.getOriginalFilename()==null ? "Evidencia adjunta" : archivo.getOriginalFilename();
        a.setTitulo(tituloAdjunto.length()>160 ? tituloAdjunto.substring(tituloAdjunto.length()-160) : tituloAdjunto);
        a.setDescripcion(limpiar(descripcion));
        ArchivoOperacion guardado=guardar(a, archivo, "evidencias-reportes");
        auditoria.registrar(AccionAuditoria.SUBIR_ARCHIVO, "ARCHIVO_OPERACION", guardado.getIdArchivo(),
                "Adjunto de observación #"+id+": "+guardado.getNombreOriginal(), null, null);
        return ArchivoOperacionResponse.desde(guardado);
    }

    @Transactional(readOnly = true)
    public List<ArchivoOperacionResponse> listarPublicos() {
        return archivoRepo.findByPublicadoTrueAndEstadoOrderByFechaSubidaDesc("ACTIVO").stream()
                .map(ArchivoOperacionResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<ArchivoOperacionResponse> listarAdministracion() {
        return archivoRepo.findByClaseOrderByFechaSubidaDesc("DOCUMENTO_PUBLICO").stream().map(ArchivoOperacionResponse::desde).toList();
    }

    @Transactional
    public ArchivoOperacionResponse subirPublico(MultipartFile archivo, String titulo, String descripcion, boolean publicado, UsuarioPrincipal actual) {
        validarArchivo(archivo);
        if (titulo==null || titulo.isBlank() || titulo.trim().length()>160) throw badRequest("El título es obligatorio y debe tener como máximo 160 caracteres.");
        if (descripcion!=null && descripcion.length()>1000) throw badRequest("La descripción supera los 1000 caracteres.");
        ArchivoOperacion a=base(archivo, actual, "DOCUMENTO_PUBLICO"); a.setTitulo(titulo.trim()); a.setDescripcion(limpiar(descripcion)); a.setPublicado(publicado);
        ArchivoOperacion guardado=guardar(a, archivo, "biblioteca-publica");
        auditoria.registrar(AccionAuditoria.SUBIR_ARCHIVO, "ARCHIVO_OPERACION", guardado.getIdArchivo(),
                "Documento compartido: "+guardado.getTitulo()+"; publicado="+publicado, null, null);
        return ArchivoOperacionResponse.desde(guardado);
    }

    @Transactional
    public ArchivoOperacionResponse actualizarPublico(Long id, ActualizarArchivoRequest req, UsuarioPrincipal actual) {
        ArchivoOperacion a=archivoRepo.findById(id).orElseThrow(()->notFound("Archivo no encontrado."));
        if (!"DOCUMENTO_PUBLICO".equals(a.getClase())) throw badRequest("Solo se pueden editar archivos de la biblioteca pública.");
        if (req.titulo()==null || req.titulo().isBlank() || req.titulo().trim().length()>160) throw badRequest("El título es obligatorio y admite hasta 160 caracteres.");
        if (req.descripcion()!=null && req.descripcion().length()>1000) throw badRequest("La descripción supera los 1000 caracteres.");
        if (req.estado()!=null && !Set.of("ACTIVO","INACTIVO").contains(req.estado().trim().toUpperCase(Locale.ROOT))) throw badRequest("Estado no válido.");
        a.setTitulo(req.titulo().trim()); a.setDescripcion(limpiar(req.descripcion())); a.setPublicado(Boolean.TRUE.equals(req.publicado()));
        if(req.estado()!=null)a.setEstado(req.estado().trim().toUpperCase(Locale.ROOT));
        a.setIdUsuarioActualiza(actual.getIdUsuario());
        ArchivoOperacion result=archivoRepo.save(a);
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO, "ARCHIVO_OPERACION", id,
                "Actualización de biblioteca pública: "+result.getTitulo()+"; publicado="+result.isPublicado()+"; estado="+result.getEstado(), null, null);
        return ArchivoOperacionResponse.desde(result);
    }

    @Transactional
    public ArchivoOperacionResponse publicarAdjunto(Long id, boolean publicado, UsuarioPrincipal actual) {
        ArchivoOperacion a=archivoRepo.findById(id).orElseThrow(()->notFound("Archivo no encontrado."));
        if (!"ADJUNTO_REPORTE".equals(a.getClase())) throw badRequest("El archivo no es evidencia de una observación.");
        a.setPublicado(publicado); a.setIdUsuarioActualiza(actual.getIdUsuario());
        ArchivoOperacion result=archivoRepo.save(a);
        auditoria.registrar(AccionAuditoria.CAMBIAR_ESTADO,"ARCHIVO_OPERACION",id,"Publicación de evidencia: "+publicado,null,null);
        return ArchivoOperacionResponse.desde(result);
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long id, UsuarioPrincipal actual, boolean publicRoute) {
        ArchivoOperacion a=archivoRepo.findById(id).orElseThrow(()->notFound("Archivo no encontrado."));
        boolean admin="ADMINISTRADOR".equals(actual.getRol());
        if (publicRoute) {
            if (!(a.isPublicado() && "ACTIVO".equals(a.getEstado())) && !admin) throw notFound("Archivo no disponible.");
            if ("ADJUNTO_REPORTE".equals(a.getClase()) && !a.isPublicado() && !admin) throw notFound("Archivo no disponible.");
        } else if ("ADJUNTO_REPORTE".equals(a.getClase())) {
            IncidenciaUsuario i=buscarIncidencia(a.getIdIncidencia()); verificarPropiedadOAdmin(i, actual);
            if (!admin && !"ACTIVO".equals(a.getEstado())) throw notFound("Archivo no disponible.");
        } else if (!admin && !(a.isPublicado() && "ACTIVO".equals(a.getEstado()))) throw notFound("Archivo no disponible.");
        return new ArchivoDescarga(a.getNombreOriginal(),a.getTipoContenido(),almacenamiento.leer(a.getRutaArchivo()));
    }

    @Transactional
    public ArchivoOperacionResponse cambiarEstadoAdjunto(Long id, String estado, UsuarioPrincipal actual) {
        ArchivoOperacion a=archivoRepo.findById(id).orElseThrow(()->notFound("Archivo no encontrado."));
        if (!Set.of("ACTIVO","INACTIVO").contains(estado==null?"":estado.trim().toUpperCase(Locale.ROOT))) throw badRequest("Estado no válido.");
        a.setEstado(estado.trim().toUpperCase(Locale.ROOT)); a.setIdUsuarioActualiza(actual.getIdUsuario());
        return ArchivoOperacionResponse.desde(archivoRepo.save(a));
    }

    private ArchivoOperacion base(MultipartFile file, UsuarioPrincipal user, String clase) {
        ArchivoOperacion a=new ArchivoOperacion(); a.setIdUsuarioSubio(user.getIdUsuario()); a.setClase(clase); a.setEstado("ACTIVO");
        String nombre=file.getOriginalFilename()==null?"archivo":file.getOriginalFilename().replace('\\','/');
        nombre=nombre.substring(nombre.lastIndexOf('/')+1); a.setNombreOriginal(nombre.length()>255?nombre.substring(nombre.length()-255):nombre);
        a.setTipoContenido(normalizarMime(file.getOriginalFilename())); a.setTamanoBytes(file.getSize()); return a;
    }
    private ArchivoOperacion guardar(ArchivoOperacion a, MultipartFile file, String carpeta) {
        try { a.setRutaArchivo(almacenamiento.guardar(carpeta,a.getNombreOriginal(),file.getInputStream())); }
        catch(IOException e){throw new UncheckedIOException("No se pudo guardar el archivo.",e);}
        return archivoRepo.save(a);
    }
    private void validarArchivo(MultipartFile f) {
        if(f==null||f.isEmpty())throw badRequest("Selecciona un archivo.");
        if(f.getSize()>MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Cada archivo admite hasta 10 MB.");
        String n=f.getOriginalFilename()==null?"":f.getOriginalFilename(); int p=n.lastIndexOf('.');
        String ext=p<0?"":n.substring(p+1).toLowerCase(Locale.ROOT);
        if(!EXTENSIONES.contains(ext))throw badRequest("Formato no permitido. Usa PDF, PNG, JPG, WebP, Excel, CSV, TXT o Word.");
    }
    private String normalizarMime(String nombre) {
        String n = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        int p = n.lastIndexOf('.'); String ext = p < 0 ? "" : n.substring(p + 1);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            default -> "application/octet-stream";
        };
    }
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
    private IncidenciaUsuario buscarIncidencia(Long id){return incidenciaRepo.findById(id).orElseThrow(()->notFound("Observación no encontrada."));}
    private void verificarPropiedadOAdmin(IncidenciaUsuario i, UsuarioPrincipal u){if(u==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);if(!"ADMINISTRADOR".equals(u.getRol())&&!i.getIdUsuarioReporta().equals(u.getIdUsuario()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo puedes consultar los archivos de tus propias observaciones.");}
    private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    private ResponseStatusException badRequest(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    public record ActualizarArchivoRequest(String titulo, String descripcion, Boolean publicado, String estado){}
    public record ArchivoDescarga(String nombre,String tipoContenido,byte[] contenido){}
}
