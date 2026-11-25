# Configurar PostgreSQL para PTAR La Enlozada

## Requisitos
- PostgreSQL 15 o superior (el contenedor incluido usa PostgreSQL 16).
- Java 17 y Maven.
- Node.js y npm para el frontend.

## Opción A: Docker Compose
1. En esta carpeta, copia `.env.example` a `.env`.
2. Cambia `POSTGRES_PASSWORD` y `JWT_SECRET` por valores propios.
3. Ejecuta `docker compose up -d db`.
4. Comprueba que la base `ptar_enlozada` esté disponible en `localhost:5432`.
5. Inicia el backend desde IntelliJ o ejecuta `docker compose up -d --build`.

## Opción B: PostgreSQL instalado localmente
1. Crea la base `ptar_enlozada` y un usuario de aplicación.
2. Configura `DB_URL=jdbc:postgresql://localhost:5432/ptar_enlozada`, `DB_USER` y `DB_PASSWORD` en la configuración de ejecución de IntelliJ.
3. Para ejecutar migraciones con Flyway, configura también `FLYWAY_USER` y `FLYWAY_PASSWORD` (pueden coincidir con el usuario configurado si tiene permisos DDL).
4. Ejecuta `pe.smcv.ptar.PtarApplication`.

## Migraciones y datos
- Flyway aplica, en orden, `V1__esquema.sql` a `V5__archivos_adjuntos_y_publicaciones.sql`.
- Hibernate usa `ddl-auto=validate`: valida el esquema y no crea ni altera tablas.
- Revisa el historial con `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`.
- No uses `docker compose down -v` si necesitas conservar los datos: elimina el volumen local de PostgreSQL.
- Si la base ya contiene tablas o datos, realiza y verifica un respaldo antes de aplicar migraciones. La migración cambia de motor; no convierte automáticamente los datos de MariaDB/MySQL a PostgreSQL.

## Variables importantes
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `FLYWAY_ENABLED`, `JPA_DDL_AUTO`, `JWT_SECRET`, `CORS_ORIGINS`, `DEMO_DATA` y `STORAGE_DIR`.
