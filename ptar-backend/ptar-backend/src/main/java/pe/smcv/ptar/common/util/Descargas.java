package pe.smcv.ptar.common.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Respuestas de descarga de archivos con nombre correcto (incluye tildes). */
public final class Descargas {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PDF = "application/pdf";

    private Descargas() {
    }

    public static ResponseEntity<byte[]> adjunto(byte[] contenido, String nombreArchivo, String tipo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(tipo != null ? tipo : "application/octet-stream"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(nombreArchivo, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(contenido.length);
        return ResponseEntity.ok().headers(headers).body(contenido);
    }
}
