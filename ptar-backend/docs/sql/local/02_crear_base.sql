-- PTAR La Enlozada - PostgreSQL local
-- Ejecutar conectado a la base de mantenimiento "postgres".
-- Ejecutar esta sentencia SOLA, fuera de cualquier bloque BEGIN/COMMIT.
-- Si la base ya existe, NO la borres ni la recrees: pasa al archivo 03_permisos_y_esquema.sql.

CREATE DATABASE ptar_enlozada
    WITH
    OWNER = ptar_backend
    ENCODING = 'UTF8'
    TEMPLATE = template0;
