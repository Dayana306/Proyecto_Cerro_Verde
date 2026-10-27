-- Consultas útiles para revisar los datos (MySQL Workbench / DBeaver).

-- Últimas 20 lecturas con su punto y parámetro
SELECT m.hora_muestreo, p.nombre_punto, pa.nombre_parametro, l.valor, pa.unidad_medida
FROM lectura_parametro l
JOIN monitoreo m        ON m.id_monitoreo = l.id_monitoreo
JOIN punto_monitoreo p  ON p.id_punto = m.id_punto
JOIN parametro pa       ON pa.id_parametro = l.id_parametro
WHERE l.estado = 'ACTIVO' AND m.estado = 'ACTIVO'
ORDER BY m.hora_muestreo DESC
LIMIT 20;

-- Alertas abiertas por nivel
SELECT nivel, COUNT(*) AS cantidad
FROM alerta
WHERE estado <> 'CERRADA'
GROUP BY nivel;

-- Umbrales vigentes (NULL en punto = umbral general)
SELECT pa.codigo, COALESCE(p.nombre_punto, 'GENERAL') AS punto,
       u.limite_min, u.limite_max, u.critico_min, u.critico_max, u.fecha_creacion
FROM umbral u
JOIN parametro pa ON pa.id_parametro = u.id_parametro
LEFT JOIN punto_monitoreo p ON p.id_punto = u.id_punto
WHERE u.estado = 'ACTIVO';

-- Promedio diario de caudal del mes actual (base del cálculo del convenio)
SELECT DATE(m.hora_muestreo) AS dia, ROUND(AVG(l.valor), 3) AS caudal_promedio
FROM lectura_parametro l
JOIN monitoreo m ON m.id_monitoreo = l.id_monitoreo
JOIN parametro pa ON pa.id_parametro = l.id_parametro
WHERE pa.codigo = 'CAUDAL' AND l.estado = 'ACTIVO' AND m.estado = 'ACTIVO'
  AND m.hora_muestreo >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
GROUP BY DATE(m.hora_muestreo)
ORDER BY dia;

-- Bitácora de hoy
SELECT h.fecha_hora, COALESCE(CONCAT(u.nombre, ' ', u.apellido), 'Sistema') AS usuario,
       h.accion, h.entidad, h.id_entidad, h.descripcion
FROM historial h
LEFT JOIN usuario u ON u.id_usuario = h.id_usuario
WHERE h.fecha_hora >= CURDATE()
ORDER BY h.fecha_hora DESC;

-- Historial de Flyway (qué migraciones se aplicaron)
SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history;
