-- V5: adjuntos en observaciones y biblioteca de archivos públicos.
-- No elimina ni modifica registros existentes. Los archivos binarios se guardan
-- en STORAGE_DIR; MariaDB conserva solamente metadatos y la ruta relativa.
CREATE TABLE IF NOT EXISTS archivo_operacion (
  id_archivo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_incidencia BIGINT NULL,
  id_usuario_subio INT NOT NULL,
  clase VARCHAR(30) NOT NULL,
  titulo VARCHAR(160) NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  ruta_archivo VARCHAR(255) NOT NULL,
  tipo_contenido VARCHAR(120) NULL,
  tamano_bytes BIGINT NOT NULL,
  descripcion VARCHAR(1000) NULL,
  publicado TINYINT(1) NOT NULL DEFAULT 0,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  fecha_subida DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  id_usuario_actualiza INT NULL,
  CONSTRAINT fk_archivo_operacion_incidencia FOREIGN KEY (id_incidencia)
    REFERENCES incidencia_usuario(id_incidencia),
  CONSTRAINT fk_archivo_operacion_usuario FOREIGN KEY (id_usuario_subio)
    REFERENCES usuario(id_usuario),
  CONSTRAINT fk_archivo_operacion_actualiza FOREIGN KEY (id_usuario_actualiza)
    REFERENCES usuario(id_usuario),
  INDEX idx_archivo_operacion_incidente (id_incidencia, fecha_subida),
  INDEX idx_archivo_operacion_publico (publicado, estado, fecha_subida),
  INDEX idx_archivo_operacion_clase (clase, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
