-- Operación, incidencias recibidas desde la web, equipos y mantenimiento.
-- Se agregan tablas nuevas sin alterar ni eliminar datos de las tablas existentes.
CREATE TABLE personal_operativo (
  id_personal INT AUTO_INCREMENT PRIMARY KEY,
  nombres VARCHAR(90) NOT NULL,
  apellidos VARCHAR(90) NOT NULL,
  cargo VARCHAR(100) NOT NULL,
  area VARCHAR(100) NOT NULL,
  turno VARCHAR(80) NOT NULL,
  telefono VARCHAR(25) NULL,
  correo VARCHAR(120) NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  es_demo BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_personal_estado_area (estado, area),
  INDEX idx_personal_nombre (apellidos, nombres)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipo_monitoreo (
  id_equipo INT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(35) NOT NULL,
  nombre VARCHAR(130) NOT NULL,
  tipo_equipo VARCHAR(80) NOT NULL,
  fabricante VARCHAR(100) NULL,
  modelo VARCHAR(100) NULL,
  modo_lectura VARCHAR(25) NOT NULL DEFAULT 'MANUAL',
  estado VARCHAR(25) NOT NULL DEFAULT 'ACTIVO',
  estado_conexion VARCHAR(25) NOT NULL DEFAULT 'NO_APLICA',
  id_punto INT NULL,
  id_personal_responsable INT NULL,
  ultima_lectura_en DATETIME NULL,
  ultima_revision DATE NULL,
  proxima_revision DATE NULL,
  observaciones VARCHAR(1000) NULL,
  es_demo BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_equipo_codigo UNIQUE (codigo),
  CONSTRAINT fk_equipo_punto FOREIGN KEY (id_punto) REFERENCES punto_monitoreo(id_punto),
  CONSTRAINT fk_equipo_personal FOREIGN KEY (id_personal_responsable) REFERENCES personal_operativo(id_personal),
  INDEX idx_equipo_estado (estado),
  INDEX idx_equipo_revision (proxima_revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE incidencia_usuario (
  id_incidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
  categoria VARCHAR(50) NOT NULL,
  titulo VARCHAR(140) NOT NULL,
  zona VARCHAR(180) NOT NULL,
  descripcion VARCHAR(4000) NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  prioridad VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
  fecha_reporte DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_usuario_reporta INT NOT NULL,
  nombre_reportante VARCHAR(170) NOT NULL,
  id_equipo INT NULL,
  id_personal_asignado INT NULL,
  fecha_hora_revision DATETIME NULL,
  resultado_resolucion VARCHAR(2000) NULL,
  CONSTRAINT fk_incidencia_usuario FOREIGN KEY (id_usuario_reporta) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_incidencia_equipo FOREIGN KEY (id_equipo) REFERENCES equipo_monitoreo(id_equipo),
  CONSTRAINT fk_incidencia_personal FOREIGN KEY (id_personal_asignado) REFERENCES personal_operativo(id_personal),
  INDEX idx_incidencia_estado_fecha (estado, fecha_reporte),
  INDEX idx_incidencia_usuario (id_usuario_reporta, fecha_reporte)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mantenimiento_programado (
  id_mantenimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_equipo INT NOT NULL,
  id_personal_asignado INT NOT NULL,
  fecha_hora_programada DATETIME NOT NULL,
  tipo VARCHAR(60) NOT NULL,
  descripcion VARCHAR(1500) NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADO',
  resultado VARCHAR(2000) NULL,
  id_usuario_creador INT NOT NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mantenimiento_equipo FOREIGN KEY (id_equipo) REFERENCES equipo_monitoreo(id_equipo),
  CONSTRAINT fk_mantenimiento_personal FOREIGN KEY (id_personal_asignado) REFERENCES personal_operativo(id_personal),
  CONSTRAINT fk_mantenimiento_creador FOREIGN KEY (id_usuario_creador) REFERENCES usuario(id_usuario),
  INDEX idx_mantenimiento_fecha_estado (fecha_hora_programada, estado),
  INDEX idx_mantenimiento_equipo (id_equipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
