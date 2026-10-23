# ptar-backend

Backend del **Sistema web de monitoreo y gestión de indicadores hídricos de la PTAR La Enlozada** (Sociedad Minera Cerro Verde).

Java 17 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA · MySQL 8 · Flyway · OpenPDF · Apache POI

## ¿Quién lee qué?

| Si eres... | Lee |
|---|---|
| Responsable de **base de datos** | [`docs/GUIA_BASE_DE_DATOS.md`](docs/GUIA_BASE_DE_DATOS.md) y [`docs/DICCIONARIO_DE_DATOS.md`](docs/DICCIONARIO_DE_DATOS.md) |
| Responsable del **frontend** | [`docs/GUIA_FRONTEND.md`](docs/GUIA_FRONTEND.md) y Swagger: <http://localhost:8080/swagger-ui.html> |
| Quien redacta la **tesis** | [`docs/DECISIONES.md`](docs/DECISIONES.md) (cambios respecto al informe) |

## Puesta en marcha

### Opción 1 — Todo con Docker (lo más simple)

Requiere [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up -d --build
```

Primera vez: 2-4 minutos (descarga dependencias y carga datos demo). Luego:

- API: <http://localhost:8080> · Estado: <http://localhost:8080/actuator/health>
- Swagger: <http://localhost:8080/swagger-ui.html>
- MySQL: `localhost:3307`, base `ptar_enlozada`, usuario `smcv_app`, contraseña `smcv_dev`

Ver logs: `docker compose logs -f backend` · Apagar: `docker compose down` · Borrar datos y empezar de cero: `docker compose down -v`

### Opción 2 — Backend desde el IDE (para desarrollar el backend)

1. Base de datos: `docker compose up -d db` (o un MySQL 8 propio, ver abajo).
2. Abrir la carpeta en **IntelliJ IDEA** (trae Maven integrado) o VS Code con *Extension Pack for Java*.
3. Ejecutar `PtarApplication` (perfil `dev` por defecto). Por consola: `mvn spring-boot:run`.

**Con MySQL propio** (XAMPP, instalación local): crea la base `ptar_enlozada` (utf8mb4) y define las variables
`DB_URL=jdbc:mysql://localhost:3306/ptar_enlozada?serverTimezone=America/Lima`, `DB_USER` y `DB_PASSWORD`
(en IntelliJ: *Run → Edit Configurations → Environment variables*).

### Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | `admin@smcv.local` | `Admin12345!` (se crea en todos los entornos; cámbiala) |
| SUPERVISOR | `supervisor@smcv.local` | `Demo12345!` (solo perfil dev) |
| GERENTE | `gerente@smcv.local` | `Demo12345!` (solo perfil dev) |
| AUDITOR | `auditor@smcv.local` | `Demo12345!` (solo perfil dev) |

En el perfil `dev`, la primera vez que arranca con la base vacía se cargan **datos de demostración**: 3 puntos de la PTAR, zonas con polígonos, umbrales de ejemplo, convenio con SEDAPAR y ~45 días de mediciones con alertas. Desactivar con `DEMO_DATA=false`.

## Funcionalidades (todas implementadas)

| Módulo | Requisitos | Qué hace |
|---|---|---|
| `auth` | RF-01 | Login con JWT, usuario actual, cambio de contraseña |
| `usuario` | RF-09 | CRUD de usuarios, roles, reseteo de contraseña |
| `ubicacion` | RF-08 | Regiones, zonas (GeoJSON), áreas, puntos de monitoreo, mapas y visor 2D con estado de alertas |
| `parametro` | RF-03, RF-06 | Parámetros con rango válido; umbrales versionados por parámetro y punto (niveles MEDIA/ALTA) |
| `monitoreo` | RF-02, RF-03 | Registro, corrección y anulación de mediciones; validación; documentos adjuntos |
| `alerta` | RF-05 | Alertas automáticas, notificaciones en panel, seguimiento y cierre |
| `dashboard` | RF-04 | Indicadores con semáforo, conteos, tendencias diarias, cumplimiento del convenio |
| `convenio` | CU10 | Entidades externas, convenio con meta de caudal y cálculo de cumplimiento |
| `reporte` | RF-07 | Reportes PDF y Excel (ambiental, auditoría, especial), historial, descarga, registro de envíos |
| `auditoria` | RF-10, RNF-04 | Bitácora de todas las operaciones, consulta y exportación a Excel |

## Estructura

```
src/main/java/pe/smcv/ptar/
├── config/       Seguridad, CORS, Swagger, almacenamiento, datos demo
├── security/     JWT, filtro, usuario autenticado
├── common/       Excepciones, formato de error, paginación, utilidades (Excel, archivos)
├── auth/  usuario/  ubicacion/  parametro/  monitoreo/
├── alerta/  dashboard/  convenio/  reporte/  auditoria/
│     └── cada módulo: controller / service / repository / entity / dto
src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
└── db/migration/   V1__esquema.sql (21 tablas), V2__datos_iniciales.sql
docs/            Guías para BD y frontend, diccionario de datos, decisiones, scripts SQL
```

Convenciones: los controladores solo exponen DTOs; permisos con `@PreAuthorize`; nada se borra físicamente (estado `INACTIVO`/`ANULADO`); reglas de negocio → HTTP 422; toda escritura queda en la bitácora.

## Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (Swagger + demo) o `prod` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | MySQL en `localhost:3307`, `smcv_app` / `smcv_dev` | Conexión a MySQL |
| `FLYWAY_ENABLED` | `true` | `false` si la base se crea manualmente |
| `JWT_SECRET` | valor de desarrollo | **Obligatorio en prod**, mínimo 32 caracteres |
| `JWT_EXPIRATION_MINUTES` | `480` | Duración de la sesión |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | URL(s) del frontend |
| `STORAGE_DIR` | `./almacenamiento` | Carpeta de documentos y reportes generados |
| `DEMO_DATA` | `true` | Cargar datos de demostración (solo dev) |

## Pruebas

```bash
mvn test
```

Incluye pruebas de la regla de alertas (`EvaluadorUmbralTest`), del cálculo del convenio (`CalculoCumplimientoTest`) y del token JWT (`JwtServiceTest`).

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `Communications link failure` al arrancar | MySQL no está levantado o el puerto no es el correcto (`docker compose up -d db`, puerto 3307) |
| `Schema-validation: missing column/table ...` | La base no coincide con las entidades. Ver `docs/GUIA_BASE_DE_DATOS.md` |
| `Validate failed: Migrations have failed validation` (Flyway) | Se editó un `V*.sql` ya aplicado. En desarrollo: `docker compose down -v` |
| El frontend recibe error de CORS | Agregar su URL a `CORS_ORIGINS` |
| 401 en todas las peticiones | Falta `Authorization: Bearer <token>` o el token venció |
| Puerto 8080 ocupado | Variable `PORT=8081` |
