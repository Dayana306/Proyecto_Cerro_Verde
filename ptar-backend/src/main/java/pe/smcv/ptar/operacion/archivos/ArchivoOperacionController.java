package pe.smcv.ptar.operacion.archivos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.smcv.ptar.common.util.Descargas;
import pe.smcv.ptar.security.UsuarioPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/operacion")
public class ArchivoOperacionController {
    private final ArchivoOperacionService service;
    public ArchivoOperacionController(ArchivoOperacionService service){this.service=service;}

    @GetMapping("/incidencias/{id}/adjuntos")
    public List<ArchivoOperacionResponse> adjuntos(@PathVariable Long id,@AuthenticationPrincipal UsuarioPrincipal actual){return service.adjuntosDeIncidencia(id,actual);}

    /** El archivo no lleva contraseña propia; la identidad del usuario autenticado se valida por JWT. */
    @PostMapping(path="/incidencias/{id}/adjuntos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoOperacionResponse adjuntar(@PathVariable Long id,@RequestParam("archivo") MultipartFile archivo,
          @RequestParam(value="descripcion",required=false) String descripcion,@AuthenticationPrincipal UsuarioPrincipal actual){
        return service.subirAdjunto(id,archivo,descripcion,actual);
    }

    @GetMapping("/archivos/{id}/descarga")
    public ResponseEntity<byte[]> descargarAdjunto(@PathVariable Long id,@AuthenticationPrincipal UsuarioPrincipal actual){
        var a=service.descargar(id,actual,false); return Descargas.adjunto(a.contenido(),a.nombre(),a.tipoContenido());
    }

    @PatchMapping("/archivos/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ArchivoOperacionResponse estadoAdjunto(@PathVariable Long id,@RequestBody EstadoRequest req,@AuthenticationPrincipal UsuarioPrincipal actual){return service.cambiarEstadoAdjunto(id,req.estado(),actual);}

    @PatchMapping("/archivos/{id}/publicacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ArchivoOperacionResponse publicarAdjunto(@PathVariable Long id,@RequestBody PublicacionRequest req,@AuthenticationPrincipal UsuarioPrincipal actual){return service.publicarAdjunto(id,Boolean.TRUE.equals(req.publicado()),actual);}

    @GetMapping("/archivos-publicos")
    public List<ArchivoOperacionResponse> archivosPublicos(){return service.listarPublicos();}

    @GetMapping("/archivos-publicos/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ArchivoOperacionResponse> archivosParaAdmin(){return service.listarAdministracion();}

    @PostMapping(path="/archivos-publicos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ArchivoOperacionResponse subirPublico(@RequestParam("archivo") MultipartFile archivo,@RequestParam("titulo") @NotBlank @Size(max=160) String titulo,
          @RequestParam(value="descripcion",required=false) @Size(max=1000) String descripcion,
          @RequestParam(defaultValue="true") boolean publicado,@AuthenticationPrincipal UsuarioPrincipal actual){return service.subirPublico(archivo,titulo,descripcion,publicado,actual);}

    @PutMapping("/archivos-publicos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ArchivoOperacionResponse actualizarPublico(@PathVariable Long id,@Valid @RequestBody ArchivoOperacionService.ActualizarArchivoRequest req,@AuthenticationPrincipal UsuarioPrincipal actual){return service.actualizarPublico(id,req,actual);}

    @GetMapping("/archivos-publicos/{id}/descarga")
    public ResponseEntity<byte[]> descargarPublico(@PathVariable Long id,@AuthenticationPrincipal UsuarioPrincipal actual){
        var a=service.descargar(id,actual,true); return Descargas.adjunto(a.contenido(),a.nombre(),a.tipoContenido());
    }

    public record EstadoRequest(@NotBlank String estado){}
    public record PublicacionRequest(Boolean publicado){}
}
