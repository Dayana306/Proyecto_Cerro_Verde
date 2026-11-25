package pe.smcv.ptar.operacion.archivos;

import java.time.LocalDateTime;

public record ArchivoOperacionResponse(Long idArchivo, Long idIncidencia, String clase, String titulo,
        String nombreOriginal, String tipoContenido, Long tamanoBytes, String descripcion,
        boolean publicado, String estado, LocalDateTime fechaSubida, String urlDescarga) {
    static ArchivoOperacionResponse desde(ArchivoOperacion a) {
        String url = "ADJUNTO_REPORTE".equals(a.getClase())
                ? "/api/operacion/archivos/" + a.getIdArchivo() + "/descarga"
                : "/api/operacion/archivos-publicos/" + a.getIdArchivo() + "/descarga";
        return new ArchivoOperacionResponse(a.getIdArchivo(), a.getIdIncidencia(), a.getClase(), a.getTitulo(),
                a.getNombreOriginal(), a.getTipoContenido(), a.getTamanoBytes(), a.getDescripcion(), a.isPublicado(),
                a.getEstado(), a.getFechaSubida(), url);
    }
}
