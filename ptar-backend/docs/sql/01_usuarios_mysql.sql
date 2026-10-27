-- =====================================================================
-- Usuarios de MySQL recomendados (ejecutar como root UNA vez).
-- Con docker-compose ya se crea smcv_app con todos los permisos sobre la BD,
-- lo cual es suficiente para el trabajo académico. Este script muestra la
-- separación de privilegios descrita en el informe (sección de seguridad).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS ptar_enlozada CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1) Usuario de migraciones (crea/modifica tablas). Lo usa Flyway: FLYWAY_USER / FLYWAY_PASSWORD.
CREATE USER IF NOT EXISTS 'smcv_migracion'@'%' IDENTIFIED BY 'cambiar_password_migracion';
GRANT ALL PRIVILEGES ON ptar_enlozada.* TO 'smcv_migracion'@'%';

-- 2) Usuario de la aplicación: solo lee, inserta y actualiza (NO borra: el sistema usa borrado lógico).
CREATE USER IF NOT EXISTS 'smcv_app'@'%' IDENTIFIED BY 'cambiar_password_app';
GRANT SELECT, INSERT, UPDATE ON ptar_enlozada.* TO 'smcv_app'@'%';

-- 3) La bitácora es inmutable: la aplicación solo puede insertar y leer.
--    (MySQL no permite quitar UPDATE de una sola tabla si se otorgó a nivel de BD;
--     para hacerlo, otorgar los permisos tabla por tabla. Para fines académicos basta con documentarlo.)

FLUSH PRIVILEGES;
