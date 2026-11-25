-- Roles recomendados para PostgreSQL. Ejecutar conectado como administrador.
-- Cambia las contraseñas antes de usar el entorno.

CREATE DATABASE ptar_enlozada;

-- Ejecutar conectado a ptar_enlozada, después de crear la base.
CREATE ROLE ptar_migracion LOGIN PASSWORD 'cambiar_password_migracion';
GRANT CONNECT ON DATABASE ptar_enlozada TO ptar_migracion;
GRANT USAGE, CREATE ON SCHEMA public TO ptar_migracion;

CREATE ROLE ptar_backend LOGIN PASSWORD 'cambiar_password_app';
GRANT CONNECT ON DATABASE ptar_enlozada TO ptar_backend;
GRANT USAGE ON SCHEMA public TO ptar_backend;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO ptar_backend;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ptar_backend;

-- Para objetos futuros creados por el usuario de migraciones:
ALTER DEFAULT PRIVILEGES FOR ROLE ptar_migracion IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE ON TABLES TO ptar_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE ptar_migracion IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO ptar_backend;
