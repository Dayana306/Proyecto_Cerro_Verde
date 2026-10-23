package pe.smcv.ptar.common.util;

import org.springframework.stereotype.Service;
import pe.smcv.ptar.common.exception.NotFoundException;
import pe.smcv.ptar.config.StorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Guarda archivos en disco (carpeta app.storage.dir). En BD solo se guarda la ruta relativa. */
@Service
public class AlmacenamientoService {

    private final Path raiz;

    public AlmacenamientoService(StorageProperties props) {
        this.raiz = Path.of(props.dir() == null || props.dir().isBlank() ? "almacenamiento" : props.dir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de almacenamiento " + raiz, e);
        }
    }

    /** Guarda y devuelve la ruta relativa (ej. "documentos/1a2b..._acta.pdf"). */
    public String guardar(String subcarpeta, String nombreOriginal, InputStream contenido) {
        String seguro = nombreSeguro(nombreOriginal);
        String relativa = subcarpeta + "/" + UUID.randomUUID() + "_" + seguro;
        Path destino = resolver(relativa);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return relativa;
    }

    public String guardar(String subcarpeta, String nombreOriginal, byte[] contenido) {
        return guardar(subcarpeta, nombreOriginal, new java.io.ByteArrayInputStream(contenido));
    }

    public byte[] leer(String rutaRelativa) {
        Path p = resolver(rutaRelativa);
        if (!Files.exists(p)) {
            throw new NotFoundException("El archivo ya no existe en el almacenamiento");
        }
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    /** Impide rutas que salgan de la carpeta raíz (ej. "../../etc/passwd"). */
    private Path resolver(String rutaRelativa) {
        Path p = raiz.resolve(rutaRelativa).normalize();
        if (!p.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        return p;
    }

    public static String nombreSeguro(String nombre) {
        String base = nombre == null || nombre.isBlank() ? "archivo" : Path.of(nombre).getFileName().toString();
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.length() > 100 ? base.substring(base.length() - 100) : base;
    }
}
