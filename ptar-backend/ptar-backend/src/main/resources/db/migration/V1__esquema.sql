-- =====================================================================
--  Sistema de monitoreo de indicadores hídricos - PTAR La Enlozada
--  V1: esquema completo (21 tablas)  ·  MySQL 8.0  ·  utf8mb4
--
--  REGLAS PARA QUIEN MANTENGA ESTE SCRIPT (ver docs/GUIA_BASE_DE_DATOS.md):
--   * Los nombres de tablas y columnas DEBEN coincidir con las entidades JPA
--     (el backend arranca con ddl-auto=validate y falla si no coinciden).
--   * Las columnas de estado/tipo son VARCHAR y guardan el nombre exacto del
--     enum Java (ej. 'ACTIVO', 'ABIERTA'). No usar ENUM de MySQL.
--   * Nada se borra físicamente: "eliminar" = cambiar estado.
--   * Una vez aplicado en una BD, este archivo NO se edita: los cambios van
--     en un V3__..., V4__..., etc.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. SEGURIDAD Y ORGANIZACIÓN
-- ---------------------------------------------------------------------
CREATE TABLE rol (
  id_rol          INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(50)  NOT NULL,                 -- ADMINISTRADOR | SUPERVISOR | GERENTE | AUDITOR
  descripcion     VARCHAR(200),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE area (
  id_area         INT AUTO_INCREMENT PRIMARY KEY,
  nombre_area     VARCHAR(100) NOT NULL,
  descripcion     VARCHAR(200),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_area_nombre UNIQUE (nombre_area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuario (
  id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(80)  NOT NULL,
  apellido        VARCHAR(80)  NOT NULL,
  correo          VARCHAR(120) NOT NULL,
  usuario_login   VARCHAR(40)  NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,                 -- BCrypt
  telefono        VARCHAR(20),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_rol          INT          NOT NULL,
  id_area         INT          NULL,
  CONSTRAINT uq_usuario_correo UNIQUE (correo),
  CONSTRAINT uq_usuario_login  UNIQUE (usuario_login),
  CONSTRAINT fk_usuario_rol  FOREIGN KEY (id_rol)  REFERENCES rol(id_rol),
  CONSTRAINT fk_usuario_area FOREIGN KEY (id_area) REFERENCES area(id_area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. UBICACIÓN (jerarquía Región > Zona > Punto de monitoreo) Y MAPA 2D
-- ---------------------------------------------------------------------
CREATE TABLE region (
  id_region       INT AUTO_INCREMENT PRIMARY KEY,
  nombre_region   VARCHAR(100) NOT NULL,
  descripcion     VARCHAR(200),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_region_nombre UNIQUE (nombre_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE zona (
  id_zona          INT AUTO_INCREMENT PRIMARY KEY,
  nombre_zona      VARCHAR(100) NOT NULL,
  descripcion      VARCHAR(200),
  tipo_zona        VARCHAR(30)  NOT NULL DEFAULT 'AREA_INFLUENCIA', -- POBLADO | COMPONENTE | AREA_INFLUENCIA
  poligono_geojson MEDIUMTEXT   NULL,                    -- GeoJSON (Polygon/MultiPolygon/Feature)
  estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_region        INT          NOT NULL,
  CONSTRAINT uq_zona_nombre UNIQUE (nombre_zona),
  CONSTRAINT fk_zona_region FOREIGN KEY (id_region) REFERENCES region(id_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE punto_monitoreo (
  id_punto        INT AUTO_INCREMENT PRIMARY KEY,
  nombre_punto    VARCHAR(100)  NOT NULL,
  descripcion     VARCHAR(200),
  latitud         DECIMAL(9,6)  NOT NULL,
  longitud        DECIMAL(9,6)  NOT NULL,
  estado          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_zona         INT           NOT NULL,
  id_area         INT           NOT NULL,
  CONSTRAINT uq_punto_nombre UNIQUE (nombre_punto),
  CONSTRAINT fk_punto_zona FOREIGN KEY (id_zona) REFERENCES zona(id_zona),
  CONSTRAINT fk_punto_area FOREIGN KEY (id_area) REFERENCES area(id_area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mapa_2d (
  id_mapa             INT AUTO_INCREMENT PRIMARY KEY,
  nombre_mapa         VARCHAR(100) NOT NULL,
  descripcion         VARCHAR(200),
  imagen_ruta         VARCHAR(255) NOT NULL,             -- URL o ruta de la imagen base del visor
  estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_actualizacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. PARÁMETROS Y UMBRALES DE ALERTA
-- ---------------------------------------------------------------------
CREATE TABLE parametro (
  id_parametro      INT AUTO_INCREMENT PRIMARY KEY,
  codigo            VARCHAR(20)    NOT NULL,             -- PH | CAUDAL | TURBIDEZ | TEMPERATURA (el backend usa CAUDAL)
  nombre_parametro  VARCHAR(60)    NOT NULL,
  descripcion       VARCHAR(200),
  unidad_medida     VARCHAR(20)    NOT NULL,
  valor_min_valido  DECIMAL(10,3)  NOT NULL,             -- rango físicamente posible (RF-03: rechaza errores de digitación)
  valor_max_valido  DECIMAL(10,3)  NOT NULL,
  estado            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVO',
  CONSTRAINT uq_parametro_codigo UNIQUE (codigo),
  CONSTRAINT ck_parametro_rango CHECK (valor_min_valido <= valor_max_valido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE umbral (
  id_umbral       INT AUTO_INCREMENT PRIMARY KEY,
  limite_min      DECIMAL(10,3)  NULL,                   -- fuera de [limite_min, limite_max]  => alerta MEDIA
  limite_max      DECIMAL(10,3)  NULL,
  critico_min     DECIMAL(10,3)  NULL,                   -- fuera de [critico_min, critico_max] => alerta ALTA
  critico_max     DECIMAL(10,3)  NULL,
  estado          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVO',  -- un cambio crea un umbral nuevo y desactiva el anterior
  fecha_creacion  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_parametro    INT            NOT NULL,
  id_punto        INT            NULL,                   -- NULL = umbral general del parámetro
  id_usuario      INT            NOT NULL,               -- quién lo configuró
  CONSTRAINT fk_umbral_parametro FOREIGN KEY (id_parametro) REFERENCES parametro(id_parametro),
  CONSTRAINT fk_umbral_punto     FOREIGN KEY (id_punto)     REFERENCES punto_monitoreo(id_punto),
  CONSTRAINT fk_umbral_usuario   FOREIGN KEY (id_usuario)   REFERENCES usuario(id_usuario),
  INDEX idx_umbral_busqueda (id_parametro, id_punto, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. MONITOREO (registro de mediciones) Y DOCUMENTOS
-- ---------------------------------------------------------------------
CREATE TABLE monitoreo (
  id_monitoreo    INT AUTO_INCREMENT PRIMARY KEY,
  tipo_muestreo   VARCHAR(30)   NOT NULL,                -- MANUAL | SEMIAUTOMATICO | SENSOR_IOT
  hora_muestreo   DATETIME      NOT NULL,                -- cuándo se tomó la muestra
  fecha_registro  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP, -- cuándo se cargó al sistema
  observacion     VARCHAR(255),
  estado          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',  -- INACTIVO = anulado
  id_punto        INT           NOT NULL,
  id_usuario      INT           NOT NULL,
  CONSTRAINT fk_monitoreo_punto   FOREIGN KEY (id_punto)   REFERENCES punto_monitoreo(id_punto),
  CONSTRAINT fk_monitoreo_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX idx_monitoreo_punto_fecha (id_punto, hora_muestreo),
  INDEX idx_monitoreo_fecha (hora_muestreo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lectura_parametro (
  id_lectura      BIGINT AUTO_INCREMENT PRIMARY KEY,
  valor           DECIMAL(10,3) NOT NULL,
  estado          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',
  id_monitoreo    INT           NOT NULL,
  id_parametro    INT           NOT NULL,
  CONSTRAINT uq_lectura_monitoreo_parametro UNIQUE (id_monitoreo, id_parametro),
  CONSTRAINT fk_lectura_monitoreo FOREIGN KEY (id_monitoreo) REFERENCES monitoreo(id_monitoreo),
  CONSTRAINT fk_lectura_parametro FOREIGN KEY (id_parametro) REFERENCES parametro(id_parametro),
  INDEX idx_lectura_parametro (id_parametro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE documento (
  id_documento     INT AUTO_INCREMENT PRIMARY KEY,
  nombre_original  VARCHAR(255) NOT NULL,
  ruta_archivo     VARCHAR(255) NOT NULL,                -- ruta relativa dentro del almacenamiento del backend
  tipo_contenido   VARCHAR(100),
  tamano_bytes     BIGINT       NOT NULL,
  descripcion      VARCHAR(255),
  estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_subida     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_monitoreo     INT          NOT NULL,
  id_usuario       INT          NOT NULL,
  CONSTRAINT fk_documento_monitoreo FOREIGN KEY (id_monitoreo) REFERENCES monitoreo(id_monitoreo),
  CONSTRAINT fk_documento_usuario   FOREIGN KEY (id_usuario)   REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 5. ALERTAS, NOTIFICACIONES Y SEGUIMIENTO
-- ---------------------------------------------------------------------
CREATE TABLE alerta (
  id_alerta        INT AUTO_INCREMENT PRIMARY KEY,
  tipo_alerta      VARCHAR(30)   NOT NULL,               -- MAXIMO_EXCEDIDO | MINIMO_EXCEDIDO
  nivel            VARCHAR(10)   NOT NULL,               -- MEDIA | ALTA
  valor_detectado  DECIMAL(10,3) NOT NULL,
  fecha_deteccion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado           VARCHAR(20)   NOT NULL DEFAULT 'ABIERTA', -- ABIERTA | EN_REVISION | CERRADA
  id_lectura       BIGINT        NOT NULL,
  id_umbral        INT           NOT NULL,               -- umbral con el que se evaluó (trazabilidad)
  CONSTRAINT fk_alerta_lectura FOREIGN KEY (id_lectura) REFERENCES lectura_parametro(id_lectura),
  CONSTRAINT fk_alerta_umbral  FOREIGN KEY (id_umbral)  REFERENCES umbral(id_umbral),
  INDEX idx_alerta_estado_nivel (estado, nivel),
  INDEX idx_alerta_fecha (fecha_deteccion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notificacion (
  id_notificacion  INT AUTO_INCREMENT PRIMARY KEY,
  mensaje          VARCHAR(255) NOT NULL,
  canal            VARCHAR(20)  NOT NULL DEFAULT 'PANEL', -- PANEL | CORREO
  fecha_envio      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_lectura    DATETIME     NULL,                    -- NULL = no leída
  id_alerta        INT          NOT NULL,
  id_usuario       INT          NOT NULL,                -- destinatario
  CONSTRAINT fk_notificacion_alerta  FOREIGN KEY (id_alerta)  REFERENCES alerta(id_alerta),
  CONSTRAINT fk_notificacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX idx_notificacion_usuario (id_usuario, fecha_lectura)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seguimiento (
  id_seguimiento   INT AUTO_INCREMENT PRIMARY KEY,
  comentario       VARCHAR(500) NOT NULL,
  estado_anterior  VARCHAR(20)  NOT NULL,
  estado_nuevo     VARCHAR(20)  NOT NULL,
  fecha            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_alerta        INT          NOT NULL,
  id_usuario       INT          NOT NULL,
  CONSTRAINT fk_seguimiento_alerta  FOREIGN KEY (id_alerta)  REFERENCES alerta(id_alerta),
  CONSTRAINT fk_seguimiento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 6. ENTIDADES EXTERNAS Y CONVENIO HÍDRICO
-- ---------------------------------------------------------------------
CREATE TABLE entidad_externa (
  id_entidad      INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(150) NOT NULL,
  tipo_entidad    VARCHAR(30)  NOT NULL,                 -- REGULADOR | FISCALIZADOR | EMPRESA_SANEAMIENTO | OTRO
  correo          VARCHAR(120),
  telefono        VARCHAR(20),
  estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_entidad_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE convenio (
  id_convenio      INT AUTO_INCREMENT PRIMARY KEY,
  nombre           VARCHAR(150) NOT NULL,
  fecha_inicio     DATE         NOT NULL,
  fecha_fin        DATE         NULL,
  caudal_meta_m3s  DECIMAL(6,3) NOT NULL,                -- 1.500 m3/s
  tipo_meta        VARCHAR(30)  NOT NULL DEFAULT 'PROMEDIO_ANUAL',
  observacion      VARCHAR(255),
  estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
  id_entidad       INT          NOT NULL,                -- contraparte (Sedapar)
  id_punto         INT          NULL,                    -- punto donde se mide el caudal del convenio (NULL = promedio de todos)
  CONSTRAINT fk_convenio_entidad FOREIGN KEY (id_entidad) REFERENCES entidad_externa(id_entidad),
  CONSTRAINT fk_convenio_punto   FOREIGN KEY (id_punto)   REFERENCES punto_monitoreo(id_punto),
  CONSTRAINT ck_convenio_fechas  CHECK (fecha_fin IS NULL OR fecha_inicio <= fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 7. REPORTES (reemplaza reporte + reporte_especial del informe)
-- ---------------------------------------------------------------------
CREATE TABLE reporte (
  id_reporte        INT AUTO_INCREMENT PRIMARY KEY,
  tipo_reporte      VARCHAR(20)  NOT NULL,               -- AMBIENTAL | AUDITORIA | ESPECIAL
  formato           VARCHAR(10)  NOT NULL,               -- PDF | XLSX
  fecha_desde       DATE         NOT NULL,
  fecha_hasta       DATE         NOT NULL,
  descripcion       VARCHAR(255),
  nombre_archivo    VARCHAR(150),
  ruta_archivo      VARCHAR(255),
  estado            VARCHAR(20)  NOT NULL DEFAULT 'GENERADO', -- GENERADO | ANULADO
  fecha_generacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_punto          INT          NULL,                   -- NULL = todas las estaciones
  id_usuario        INT          NOT NULL,
  CONSTRAINT fk_reporte_punto   FOREIGN KEY (id_punto)   REFERENCES punto_monitoreo(id_punto),
  CONSTRAINT fk_reporte_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_reporte_fechas CHECK (fecha_desde <= fecha_hasta),
  INDEX idx_reporte_fecha (fecha_generacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reporte_detalle (
  id_detalle         INT AUTO_INCREMENT PRIMARY KEY,
  cantidad_lecturas  INT           NOT NULL,
  valor_promedio     DECIMAL(10,3) NULL,
  valor_minimo       DECIMAL(10,3) NULL,
  valor_maximo       DECIMAL(10,3) NULL,
  cantidad_alertas   INT           NOT NULL DEFAULT 0,
  id_reporte         INT           NOT NULL,
  id_parametro       INT           NOT NULL,
  id_punto           INT           NOT NULL,
  CONSTRAINT fk_detalle_reporte   FOREIGN KEY (id_reporte)   REFERENCES reporte(id_reporte),
  CONSTRAINT fk_detalle_parametro FOREIGN KEY (id_parametro) REFERENCES parametro(id_parametro),
  CONSTRAINT fk_detalle_punto     FOREIGN KEY (id_punto)     REFERENCES punto_monitoreo(id_punto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reporte_entidad_externa (
  id_envio      INT AUTO_INCREMENT PRIMARY KEY,
  fecha_envio   DATETIME     NOT NULL,
  medio         VARCHAR(30)  NOT NULL,                   -- CORREO | MESA_DE_PARTES | PLATAFORMA_VIRTUAL | OTRO
  observacion   VARCHAR(255),
  id_reporte    INT          NOT NULL,
  id_entidad    INT          NOT NULL,
  id_usuario    INT          NOT NULL,
  CONSTRAINT fk_envio_reporte FOREIGN KEY (id_reporte) REFERENCES reporte(id_reporte),
  CONSTRAINT fk_envio_entidad FOREIGN KEY (id_entidad) REFERENCES entidad_externa(id_entidad),
  CONSTRAINT fk_envio_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 8. AUDITORÍA (bitácora de solo inserción, RF-10 / RNF-04)
-- ---------------------------------------------------------------------
CREATE TABLE historial (
  id_historial      BIGINT AUTO_INCREMENT PRIMARY KEY,
  fecha_hora        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accion            VARCHAR(30)  NOT NULL,               -- CREAR | MODIFICAR | CAMBIAR_ESTADO | LOGIN | ...
  entidad           VARCHAR(40)  NOT NULL,               -- USUARIO | MONITOREO | UMBRAL | REPORTE | ...
  id_entidad        BIGINT       NULL,
  descripcion       VARCHAR(255),
  datos_anteriores  TEXT         NULL,                   -- JSON
  datos_nuevos      TEXT         NULL,                   -- JSON
  ip_origen         VARCHAR(45),
  id_usuario        INT          NULL,                   -- NULL: sistema o login fallido
  CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX idx_historial_entidad (entidad, id_entidad),
  INDEX idx_historial_fecha (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
