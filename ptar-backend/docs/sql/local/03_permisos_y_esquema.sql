-- PTAR La Enlozada - PostgreSQL local
-- IMPORTANTE: abrir Query Tool conectado a la base ptar_enlozada (no a postgres).

ALTER SCHEMA public OWNER TO ptar_backend;
GRANT ALL PRIVILEGES ON SCHEMA public TO ptar_backend;
GRANT CONNECT, TEMPORARY ON DATABASE ptar_enlozada TO ptar_backend;

-- Permisos sobre objetos existentes (si los hubiera).
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ptar_backend;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ptar_backend;
GRANT ALL PRIVILEGES ON ALL ROUTINES IN SCHEMA public TO ptar_backend;

-- Permisos por defecto para los objetos nuevos creados por ptar_backend/Flyway.
ALTER DEFAULT PRIVILEGES FOR ROLE ptar_backend IN SCHEMA public
    GRANT ALL PRIVILEGES ON TABLES TO ptar_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE ptar_backend IN SCHEMA public
    GRANT ALL PRIVILEGES ON SEQUENCES TO ptar_backend;
ALTER DEFAULT PRIVILEGES FOR ROLE ptar_backend IN SCHEMA public
    GRANT ALL PRIVILEGES ON ROUTINES TO ptar_backend;

ALTER ROLE ptar_backend IN DATABASE ptar_enlozada
    SET search_path = public;
