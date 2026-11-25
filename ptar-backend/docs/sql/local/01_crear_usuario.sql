-- PTAR La Enlozada - PostgreSQL local
-- Ejecutar conectado a la base de mantenimiento "postgres" con un usuario administrador.
-- Ejecutar UNA SOLA VEZ. Si el rol ya existe, usar la sentencia ALTER ROLE indicada abajo.

CREATE ROLE ptar_backend
    WITH LOGIN
    PASSWORD '123456';

-- Si ptar_backend YA EXISTE, no vuelvas a ejecutar CREATE ROLE.
-- En ese caso ejecuta esta sentencia por separado:
-- ALTER ROLE ptar_backend WITH LOGIN PASSWORD '123456';
