-- =====================================================================
-- V2: datos mínimos para que el sistema funcione (en cualquier entorno).
-- Los datos de DEMOSTRACIÓN (puntos, umbrales, mediciones...) NO van aquí:
-- los carga el backend al arrancar con el perfil "dev" (DevDataInitializer).
-- =====================================================================

-- Roles: los nombres deben coincidir con los usados en @PreAuthorize del backend.
INSERT INTO rol (nombre, descripcion) VALUES
  ('ADMINISTRADOR', 'Gestiona usuarios, estaciones, parámetros y umbrales de alerta'),
  ('SUPERVISOR',    'Supervisor de planta: registra y corrige mediciones'),
  ('GERENTE',       'Gerente de Medio Ambiente: dashboard, alertas y reportes'),
  ('AUDITOR',       'Auditor (ANA/OEFA): consulta, reportes y bitácora de solo lectura');

-- Área base para el administrador inicial.
INSERT INTO area (nombre_area, descripcion) VALUES
  ('Gerencia de Medio Ambiente', 'Responsable del monitoreo hídrico de la PTAR');

-- Administrador inicial. Contraseña: Admin12345!  (BCrypt)  -> CAMBIARLA tras el primer ingreso.
INSERT INTO usuario (nombre, apellido, correo, usuario_login, password_hash, id_rol, id_area)
VALUES ('Administrador', 'Sistema', 'admin@smcv.local', 'admin',
        '$2a$10$5kcKLsIBk2Buu80c2gQAOuXjtXVOVxph0f8Rhc8SdvJZMcslGf8cm',
        (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR'),
        (SELECT id_area FROM area WHERE nombre_area = 'Gerencia de Medio Ambiente'));

-- Parámetros monitoreados. El código CAUDAL lo usa el backend para el convenio.
-- Rangos de validez física: valores amplios y referenciales (ajustables desde la API).
INSERT INTO parametro (codigo, nombre_parametro, descripcion, unidad_medida, valor_min_valido, valor_max_valido) VALUES
  ('PH',          'pH',          'Potencial de hidrógeno del agua tratada', 'pH',   0.000,   14.000),
  ('CAUDAL',      'Caudal',      'Caudal de agua tratada',                  'm3/s', 0.000,   20.000),
  ('TURBIDEZ',    'Turbidez',    'Turbidez del agua tratada',               'NTU',  0.000, 1000.000),
  ('TEMPERATURA', 'Temperatura', 'Temperatura del agua tratada',            '°C',   0.000,   50.000);

-- Entidades externas con las que interactúa la empresa.
INSERT INTO entidad_externa (nombre, tipo_entidad, correo) VALUES
  ('SEDAPAR',                                         'EMPRESA_SANEAMIENTO', NULL),
  ('Autoridad Nacional del Agua (ANA)',               'REGULADOR',           NULL),
  ('Organismo de Evaluación y Fiscalización Ambiental (OEFA)', 'FISCALIZADOR', NULL);
