package pe.smcv.ptar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.storage.dir: carpeta donde el backend guarda documentos y reportes generados. */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String dir) {
}
