# Diccionario de datos

Generado a partir de `src/main/resources/db/migration/V1__esquema.sql`. Si cambia el script, este archivo debe actualizarse.

## rol

Roles del sistema. El backend usa los nombres exactos en sus permisos.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_rol` | INT | No |  | PK, autoincremental. |
| `nombre` | VARCHAR(50) | No |  | ADMINISTRADOR / SUPERVISOR / GERENTE / AUDITOR |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |

**Únicos:** (nombre)

## area

Áreas organizacionales (a qué área pertenece un usuario o un punto).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_area` | INT | No |  | PK, autoincremental. |
| `nombre_area` | VARCHAR(100) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |

**Únicos:** (nombre_area)

## usuario

Usuarios del sistema. La contraseña se guarda cifrada con BCrypt.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_usuario` | INT | No |  | PK, autoincremental. |
| `nombre` | VARCHAR(80) | No |  |  |
| `apellido` | VARCHAR(80) | No |  |  |
| `correo` | VARCHAR(120) | No |  |  |
| `usuario_login` | VARCHAR(40) | No |  |  |
| `password_hash` | VARCHAR(255) | No |  | BCrypt |
| `telefono` | VARCHAR(20) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_rol` | INT | No |  |  |
| `id_area` | INT | Sí |  |  |

**Claves foráneas:** `id_area` → `area.id_area`

**Únicos:** (correo), (usuario_login)

## region

Nivel superior de la jerarquía geográfica.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_region` | INT | No |  | PK, autoincremental. |
| `nombre_region` | VARCHAR(100) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |

**Únicos:** (nombre_region)

## zona

Zonas de afectación (poblados, componentes, áreas de influencia) con su polígono GeoJSON para el mapa 2D.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_zona` | INT | No |  | PK, autoincremental. |
| `nombre_zona` | VARCHAR(100) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `tipo_zona` | VARCHAR(30) | No | 'AREA_INFLUENCIA' | POBLADO / COMPONENTE / AREA_INFLUENCIA |
| `poligono_geojson` | MEDIUMTEXT | Sí |  | GeoJSON (Polygon/MultiPolygon/Feature) |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_region` | INT | No |  |  |

**Claves foráneas:** `id_region` → `region.id_region`

**Únicos:** (nombre_zona)

## punto_monitoreo

Estaciones donde se toman las muestras.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_punto` | INT | No |  | PK, autoincremental. |
| `nombre_punto` | VARCHAR(100) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `latitud` | DECIMAL(9,6) | No |  |  |
| `longitud` | DECIMAL(9,6) | No |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_zona` | INT | No |  |  |
| `id_area` | INT | No |  |  |

**Claves foráneas:** `id_zona` → `zona.id_zona`, `id_area` → `area.id_area`

**Únicos:** (nombre_punto)

## mapa_2d

Imagen base del visor 2D.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_mapa` | INT | No |  | PK, autoincremental. |
| `nombre_mapa` | VARCHAR(100) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `imagen_ruta` | VARCHAR(255) | No |  | URL o ruta de la imagen base del visor |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_actualizacion` | DATETIME | No | CURRENT_TIMESTAMP |  |

## parametro

Parámetros medidos y su rango físicamente válido (RF-03).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_parametro` | INT | No |  | PK, autoincremental. |
| `codigo` | VARCHAR(20) | No |  | PH / CAUDAL / TURBIDEZ / TEMPERATURA (el backend usa CAUDAL) |
| `nombre_parametro` | VARCHAR(60) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `unidad_medida` | VARCHAR(20) | No |  |  |
| `valor_min_valido` | DECIMAL(10,3) | No |  | rango físicamente posible (RF-03: rechaza errores de digitación) |
| `valor_max_valido` | DECIMAL(10,3) | No |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |

**Únicos:** (codigo)

**Validaciones (CHECK):** `(valor_min_valido <= valor_max_valido)`

## umbral

Límites de alerta por parámetro (y opcionalmente por punto). Versionado: solo uno ACTIVO por parámetro+punto.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_umbral` | INT | No |  | PK, autoincremental. |
| `limite_min` | DECIMAL(10,3) | Sí |  | fuera de [limite_min, limite_max]  => alerta MEDIA |
| `limite_max` | DECIMAL(10,3) | Sí |  |  |
| `critico_min` | DECIMAL(10,3) | Sí |  | fuera de [critico_min, critico_max] => alerta ALTA |
| `critico_max` | DECIMAL(10,3) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' | un cambio crea un umbral nuevo y desactiva el anterior |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_parametro` | INT | No |  |  |
| `id_punto` | INT | Sí |  | NULL = umbral general del parámetro |
| `id_usuario` | INT | No |  | quién lo configuró |

**Claves foráneas:** `id_parametro` → `parametro.id_parametro`

**Índices:** `INDEX idx_umbral_busqueda (id_parametro, id_punto, estado)`

## monitoreo

Un muestreo en un punto y momento. Agrupa lecturas.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_monitoreo` | INT | No |  | PK, autoincremental. |
| `tipo_muestreo` | VARCHAR(30) | No |  | MANUAL / SEMIAUTOMATICO / SENSOR_IOT |
| `hora_muestreo` | DATETIME | No |  | cuándo se tomó la muestra |
| `fecha_registro` | DATETIME | No | CURRENT_TIMESTAMP | cuándo se cargó al sistema |
| `observacion` | VARCHAR(255) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' | INACTIVO = anulado |
| `id_punto` | INT | No |  |  |
| `id_usuario` | INT | No |  |  |

**Claves foráneas:** `id_usuario` → `usuario.id_usuario`

**Índices:** `INDEX idx_monitoreo_punto_fecha (id_punto, hora_muestreo)`; `INDEX idx_monitoreo_fecha (hora_muestreo)`

## lectura_parametro

Valor de un parámetro en un monitoreo. Tabla central del modelo.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_lectura` | BIGINT | No |  | PK, autoincremental. |
| `valor` | DECIMAL(10,3) | No |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `id_monitoreo` | INT | No |  |  |
| `id_parametro` | INT | No |  |  |

**Claves foráneas:** `id_monitoreo` → `monitoreo.id_monitoreo`, `id_parametro` → `parametro.id_parametro`

**Únicos:** (id_monitoreo, id_parametro)

**Índices:** `INDEX idx_lectura_parametro (id_parametro)`

## documento

Archivos de respaldo de un monitoreo (el archivo está en disco; aquí solo la ruta).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_documento` | INT | No |  | PK, autoincremental. |
| `nombre_original` | VARCHAR(255) | No |  |  |
| `ruta_archivo` | VARCHAR(255) | No |  | ruta relativa dentro del almacenamiento del backend |
| `tipo_contenido` | VARCHAR(100) | Sí |  |  |
| `tamano_bytes` | BIGINT | No |  |  |
| `descripcion` | VARCHAR(255) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_subida` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_monitoreo` | INT | No |  |  |
| `id_usuario` | INT | No |  |  |

**Claves foráneas:** `id_monitoreo` → `monitoreo.id_monitoreo`

## alerta

Alerta generada automáticamente cuando una lectura excede su umbral.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_alerta` | INT | No |  | PK, autoincremental. |
| `tipo_alerta` | VARCHAR(30) | No |  | MAXIMO_EXCEDIDO / MINIMO_EXCEDIDO |
| `nivel` | VARCHAR(10) | No |  | MEDIA / ALTA |
| `valor_detectado` | DECIMAL(10,3) | No |  |  |
| `fecha_deteccion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `estado` | VARCHAR(20) | No | 'ABIERTA' | ABIERTA / EN_REVISION / CERRADA |
| `id_lectura` | BIGINT | No |  |  |
| `id_umbral` | INT | No |  | umbral con el que se evaluó (trazabilidad) |

**Claves foráneas:** `id_lectura` → `lectura_parametro.id_lectura`

**Índices:** `INDEX idx_alerta_estado_nivel (estado, nivel)`; `INDEX idx_alerta_fecha (fecha_deteccion)`

## notificacion

Aviso de una alerta a un usuario (campana del panel).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_notificacion` | INT | No |  | PK, autoincremental. |
| `mensaje` | VARCHAR(255) | No |  |  |
| `canal` | VARCHAR(20) | No | 'PANEL' | PANEL / CORREO |
| `fecha_envio` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `fecha_lectura` | DATETIME | Sí |  | NULL = no leída |
| `id_alerta` | INT | No |  |  |
| `id_usuario` | INT | No |  | destinatario |

**Claves foráneas:** `id_usuario` → `usuario.id_usuario`

**Índices:** `INDEX idx_notificacion_usuario (id_usuario, fecha_lectura)`

## seguimiento

Acciones y cambios de estado sobre una alerta.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_seguimiento` | INT | No |  | PK, autoincremental. |
| `comentario` | VARCHAR(500) | No |  |  |
| `estado_anterior` | VARCHAR(20) | No |  |  |
| `estado_nuevo` | VARCHAR(20) | No |  |  |
| `fecha` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_alerta` | INT | No |  |  |
| `id_usuario` | INT | No |  |  |

**Claves foráneas:** `id_usuario` → `usuario.id_usuario`

## entidad_externa

SEDAPAR, ANA, OEFA y otras instituciones.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_entidad` | INT | No |  | PK, autoincremental. |
| `nombre` | VARCHAR(150) | No |  |  |
| `tipo_entidad` | VARCHAR(30) | No |  | REGULADOR / FISCALIZADOR / EMPRESA_SANEAMIENTO / OTRO |
| `correo` | VARCHAR(120) | Sí |  |  |
| `telefono` | VARCHAR(20) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `fecha_creacion` | DATETIME | No | CURRENT_TIMESTAMP |  |

**Únicos:** (nombre)

## convenio

Convenio hídrico con su meta de caudal.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_convenio` | INT | No |  | PK, autoincremental. |
| `nombre` | VARCHAR(150) | No |  |  |
| `fecha_inicio` | DATE | No |  |  |
| `fecha_fin` | DATE | Sí |  |  |
| `caudal_meta_m3s` | DECIMAL(6,3) | No |  | 1.500 m3/s |
| `tipo_meta` | VARCHAR(30) | No | 'PROMEDIO_ANUAL' |  |
| `observacion` | VARCHAR(255) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'ACTIVO' |  |
| `id_entidad` | INT | No |  | contraparte (Sedapar) |
| `id_punto` | INT | Sí |  | punto donde se mide el caudal del convenio (NULL = promedio de todos) |

**Claves foráneas:** `id_entidad` → `entidad_externa.id_entidad`

**Validaciones (CHECK):** `(fecha_fin IS NULL OR fecha_inicio <= fecha_fin)`

## reporte

Reportes PDF/Excel generados (el archivo está en disco).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_reporte` | INT | No |  | PK, autoincremental. |
| `tipo_reporte` | VARCHAR(20) | No |  | AMBIENTAL / AUDITORIA / ESPECIAL |
| `formato` | VARCHAR(10) | No |  | PDF / XLSX |
| `fecha_desde` | DATE | No |  |  |
| `fecha_hasta` | DATE | No |  |  |
| `descripcion` | VARCHAR(255) | Sí |  |  |
| `nombre_archivo` | VARCHAR(150) | Sí |  |  |
| `ruta_archivo` | VARCHAR(255) | Sí |  |  |
| `estado` | VARCHAR(20) | No | 'GENERADO' | GENERADO / ANULADO |
| `fecha_generacion` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `id_punto` | INT | Sí |  | NULL = todas las estaciones |
| `id_usuario` | INT | No |  |  |

**Claves foráneas:** `id_usuario` → `usuario.id_usuario`

**Validaciones (CHECK):** `(fecha_desde <= fecha_hasta)`

**Índices:** `INDEX idx_reporte_fecha (fecha_generacion)`

## reporte_detalle

Estadísticas consolidadas por punto y parámetro de cada reporte.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_detalle` | INT | No |  | PK, autoincremental. |
| `cantidad_lecturas` | INT | No |  |  |
| `valor_promedio` | DECIMAL(10,3) | Sí |  |  |
| `valor_minimo` | DECIMAL(10,3) | Sí |  |  |
| `valor_maximo` | DECIMAL(10,3) | Sí |  |  |
| `cantidad_alertas` | INT | No | 0 |  |
| `id_reporte` | INT | No |  |  |
| `id_parametro` | INT | No |  |  |
| `id_punto` | INT | No |  |  |

**Claves foráneas:** `id_parametro` → `parametro.id_parametro`

## reporte_entidad_externa

Constancia de entrega de un reporte a una entidad externa.

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_envio` | INT | No |  | PK, autoincremental. |
| `fecha_envio` | DATETIME | No |  |  |
| `medio` | VARCHAR(30) | No |  | CORREO / MESA_DE_PARTES / PLATAFORMA_VIRTUAL / OTRO |
| `observacion` | VARCHAR(255) | Sí |  |  |
| `id_reporte` | INT | No |  |  |
| `id_entidad` | INT | No |  |  |
| `id_usuario` | INT | No |  |  |

**Claves foráneas:** `id_reporte` → `reporte.id_reporte`, `id_entidad` → `entidad_externa.id_entidad`, `id_usuario` → `usuario.id_usuario`

## historial

Bitácora de auditoría. Solo inserción (RF-10, RNF-04).

| Columna | Tipo | Nulo | Por defecto | Descripción |
|---|---|---|---|---|
| `id_historial` | BIGINT | No |  | PK, autoincremental. |
| `fecha_hora` | DATETIME | No | CURRENT_TIMESTAMP |  |
| `accion` | VARCHAR(30) | No |  | CREAR / MODIFICAR / CAMBIAR_ESTADO / LOGIN / ... |
| `entidad` | VARCHAR(40) | No |  | USUARIO / MONITOREO / UMBRAL / REPORTE / ... |
| `id_entidad` | BIGINT | Sí |  |  |
| `descripcion` | VARCHAR(255) | Sí |  |  |
| `datos_anteriores` | TEXT | Sí |  | JSON |
| `datos_nuevos` | TEXT | Sí |  | JSON |
| `ip_origen` | VARCHAR(45) | Sí |  |  |
| `id_usuario` | INT | Sí |  | NULL: sistema o login fallido |

**Claves foráneas:** `id_usuario` → `usuario.id_usuario`

**Índices:** `INDEX idx_historial_entidad (entidad, id_entidad)`; `INDEX idx_historial_fecha (fecha_hora)`
