-- Ejecutar conectado a ptar_enlozada.

SELECT current_database() AS base_actual,
       current_user AS usuario_actual,
       version() AS version_postgresql;

SELECT schemaname, tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- Flyway debe aparecer después de iniciar Spring Boot por primera vez.
SELECT installed_rank, version, description, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
