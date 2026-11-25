# ptar-backend

Backend del **Sistema web de monitoreo y gestión de indicadores hídricos de la PTAR La Enlozada** (Sociedad Minera Cerro Verde).

Java 17 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA · **PostgreSQL 15+** · Flyway · OpenPDF · Apache POI

## ¿Quién lee qué?

| Si eres... | Lee |
|---|---|
| Responsable de **base de datos** | [`docs/GUIA_BASE_DE_DATOS.md`](docs/GUIA_BASE_DE_DATOS.md) y [`docs/DICCIONARIO_DE_DATOS.md`](docs/DICCIONARIO_DE_DATOS.md) |
| Responsable del **frontend** | [`docs/GUIA_FRONTEND.md`](docs/GUIA_FRONTEND.md) y Swagger: <http://localhost:8081/swagger-ui.html> |
| Quien redacta la **tesis** | [`docs/DECISIONES.md`](docs/DECISIONES.md) |

## Puesta en marcha

Consulta [`INSTRUCCIONES_POSTGRESQL.md`](INSTRUCCIONES_POSTGRESQL.md) para configurar PostgreSQL instalado localmente o mediante Docker.

### Opción 1 — PostgreSQL y backend con Docker

Requiere Docker Desktop. Copia `.env.example` a `.env`, cambia las contraseñas y ejecuta:

```bash
docker compose up -d --build
```

Servicios locales:
- API: <http://localhost:8081>
- Estado: <http://localhost:8081/actuator/health>
- Swagger: <http://localhost:8081/swagger-ui.html>
- PostgreSQL: `localhost:5432`, base `ptar_enlozada` (credenciales configuradas en `.env`).

Ver logs: `docker compose logs -f backend` · Apagar: `docker compose down`.
**No ejecutes `docker compose down -v` si necesitas conservar los datos**: ese comando elimina el volumen de PostgreSQL.

### Opción 2 — Backend desde IntelliJ IDEA

1. Inicia PostgreSQL y crea la base `ptar_enlozada` junto con un usuario con permisos para ejecutar migraciones.
2. Configura en **Run → Edit Configurations**: `DB_URL=jdbc:postgresql://localhost:5432/ptar_enlozada`, `DB_USER=ptar_backend`, `DB_PASSWORD=123456`, `FLYWAY_USER=ptar_backend` y `FLYWAY_PASSWORD=123456` (solo práctica local).
3. Abre `ptar-backend` en IntelliJ IDEA y ejecuta `PtarApplication` con el perfil `dev` (por defecto), o ejecuta `mvn spring-boot:run`.
4. Flyway crea el esquema y carga los datos iniciales; Hibernate valida el esquema mediante `ddl-auto=validate`.

### Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | `admin@smcv.local` | `Admin12345!` (cámbiala tras el primer ingreso) |
| SUPERVISOR | `supervisor@smcv.local` | `Demo12345!` (solo perfil dev, si se habilitan datos demo) |
| GERENTE | `gerente@smcv.local` | `Demo12345!` (solo perfil dev, si se habilitan datos demo) |
| AUDITOR | `auditor@smcv.local` | `Demo12345!` (solo perfil dev, si se habilitan datos demo) |

La carga de datos de demostración se controla con `DEMO_DATA`; el valor del proyecto se mantiene en `false` por defecto.

## Funcionalidades

| Módulo | Requisitos | Qué hace |
|---|---|---|
| `auth` | RF-01 | Login con JWT, usuario actual, cambio y recuperación de contraseña |
| `usuario` | RF-09 | Gestión de usuarios, roles y restablecimiento de contraseña |
| `ubicacion` | RF-08 | Regiones, zonas (GeoJSON), áreas, puntos de monitoreo y mapas |
| `parametro` | RF-03, RF-06 | Parámetros, rangos válidos y umbrales versionados |
| `monitoreo` | RF-02, RF-03 | Registro, corrección y anulación de mediciones, validación y documentos |
| `alerta` | RF-05 | Alertas automáticas, notificaciones, seguimiento y cierre |
| `dashboard` | RF-04 | Indicadores, conteos, tendencias y cumplimiento de convenio |
| `convenio` | CU10 | Entidades externas, convenios y cálculo de cumplimiento |
| `reporte` | RF-07 | Reportes PDF/Excel/PowerPoint, historial, descargas y envíos |
| `auditoria` | RF-10, RNF-04 | Bitácora, consulta y exportación |

## Variables de entorno

| Variable | Valor de referencia | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` o `prod` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ptar_enlozada` | URL JDBC PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `ptar_backend` / `123456` | Credenciales locales de aplicación y migración; cambia la contraseña fuera de prácticas locales |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | `ptar_backend` / `123456` | Credenciales Flyway para el entorno local |
| `FLYWAY_ENABLED` | `true` | Habilita migraciones versionadas |
| `JPA_DDL_AUTO` | `validate` | Hibernate valida; no crea el esquema |
| `JWT_SECRET` | valor de desarrollo | En producción, secreto propio de al menos 32 caracteres |
| `JWT_EXPIRATION_MINUTES` | `480` | Duración del token |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Orígenes permitidos |
| `STORAGE_DIR` | `./almacenamiento` | Archivos y reportes generados |
| `DEMO_DATA` | `false` | Carga datos de demostración solo en perfil dev |

### Gestión de usuarios del administrador

La pantalla **Gestión de usuarios** consulta `GET /api/usuarios/todos`, un listado completo protegido por el rol `ADMINISTRADOR` y sin depender de parámetros de paginación. Devuelve los registros existentes de la tabla `usuario` (sin hashes de contraseña). La edición, activación/desactivación y restablecimiento administrativo siguen usando las rutas `PUT /api/usuarios/{id}`, `PATCH /api/usuarios/{id}/estado` y `PUT /api/usuarios/{id}/password`. Esta corrección no requiere migración ni borra cuentas existentes.

## Validaciones

El backend valida los cuerpos de entrada con Jakarta Bean Validation y reglas de negocio en los servicios. React valida los formularios antes de enviarlos y muestra los errores devueltos por la API. La validación del navegador no sustituye la validación del servidor.

## Pruebas

```bash
mvn test
```

Las pruebas incluidas cubren reglas de umbrales, cálculo de cumplimiento, JWT y validaciones cruzadas de DTO (`ValidacionDtosTest`).

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| No conecta a PostgreSQL | Comprueba el servicio PostgreSQL, el puerto 5432, `DB_URL`, `DB_USER` y `DB_PASSWORD`. |
| `password authentication failed` | Verifica el usuario/contraseña y los permisos de conexión en PostgreSQL. |
| `database "ptar_enlozada" does not exist` | Crea la base con ese nombre o corrige `DB_URL`. |
| `Schema-validation: missing column/table ...` | El esquema no coincide con las entidades. Revisa las migraciones Flyway. |
| Error de checksum Flyway | Se modificó una migración ya aplicada. En una base con datos, no borres el historial: restaura el archivo original o prepara una migración nueva y respalda antes. |
| Error CORS | Agrega el origen del frontend a `CORS_ORIGINS`. |
| 401 en la API | Comprueba el token Bearer y que la sesión siga vigente. |
| Puerto 8081 ocupado | Cambia `PORT` y actualiza `VITE_API_URL` en el frontend. |


### Formatos de reporte

El endpoint POST `/api/reportes` admite `formato`: `PDF`, `XLSX` o `PPTX`. El archivo generado se descarga mediante GET `/api/reportes/{id}/descarga`.

Ejemplo de cuerpo para generar una presentación PowerPoint:

```json
{
  "tipoReporte": "AMBIENTAL",
  "formato": "PPTX",
  "fechaDesde": "2026-01-01",
  "fechaHasta": "2026-01-31",
  "idPunto": null,
  "descripcion": "Reporte ambiental del periodo"
}
```

Para editar la presentación en Canva, descarga el archivo `.pptx` y usa la opción de subir/importar en Canva desde el navegador. La importación no se realiza automáticamente desde la API.

### Recuperación de contraseña por código de correo

La pantalla solicita el correo y el backend envía un código de un solo uso mediante Jakarta Mail. Después, el usuario ingresa el código de 8 dígitos, la nueva contraseña y su confirmación. El código vence en 10 minutos y admite hasta 5 intentos. El cambio solo se ejecuta si el código es válido, vigente y corresponde a una cuenta activa. Configura las variables `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_AUTH`, `MAIL_STARTTLS` y `MAIL_SSL` indicadas en `CONFIGURAR_CORREO_SMTP.md`.

### Notificaciones por correo

El módulo **Notificaciones** está disponible para el rol `ADMINISTRADOR`. El endpoint `POST /api/notificaciones/correos` acepta datos multipart, contenido de texto o HTML y hasta 5 archivos adjuntos de 10 MB cada uno. `GET /api/notificaciones/correos` consulta los intentos guardados en PostgreSQL; `GET /api/notificaciones/correos/configuracion` informa el estado de SMTP sin exponer secretos. Cada intento queda en la tabla `correo_notificacion` y se registra también en la bitácora de auditoría. La lista de destinatarios viaja en copia oculta (BCC) para que no se expongan mutuamente sus direcciones.

### Alertas publicadas por administración

La migración `V7__alertas_publicadas_y_lectura_por_usuario.sql` agrega las tablas para avisos manuales y el estado de lectura individual. El administrador publica desde **Centro de alertas → Agregar alerta**; cada cuenta ve el aviso y puede marcarlo como visto. El contador rojo de avisos no vistos y el estado Vista/No vista son personales y se muestran tanto al administrador como a cada usuario. No aparece contador cuando la cuenta no tiene avisos publicados pendientes. Los eventos automáticos de umbrales se mantienen separados y no activan este contador.

## Correcciones adicionales de interfaz y consultas

La entrega actual incluye los siguientes flujos, sin añadir una migración destructiva:

- `GET /api/usuarios/todos`: solo ADMINISTRADOR; lista todas las cuentas con sus roles cargados, incluidas las inactivas. La respuesta no contiene hashes de contraseñas.
- `GET /api/puntos-monitoreo`: consulta de estaciones. `POST` y `PUT /api/puntos-monitoreo/{id}` y `PATCH /api/puntos-monitoreo/{id}/estado` están limitados a ADMINISTRADOR.
- `GET /api/monitoreos`: consulta paginada del historial. `POST` y `PUT /api/monitoreos/{id}` escriben lecturas y están limitados a ADMINISTRADOR/SUPERVISOR.
- `GET /api/alertas-publicadas`: devuelve las alertas publicadas con su estado de lectura para el usuario autenticado. `POST` publica avisos (ADMINISTRADOR) y `PATCH /api/alertas-publicadas/{id}/vista` marca la alerta como vista para la cuenta autenticada.

El estado Vista/No vista es individual por cuenta, también para la cuenta administradora. El frontend muestra el contador rojo exclusivamente para avisos publicados por administración que la cuenta autenticada aún no ha visto. Los eventos automáticos derivados de umbrales se muestran como eventos de medición separados y no generan ese contador. La sección Configuración solo se muestra para el rol ADMINISTRADOR.

No es necesario alterar las filas existentes de usuarios, estaciones o mediciones. Para que la pantalla muestre los cinco usuarios, inicia sesión con la cuenta que tenga el rol `ADMINISTRADOR` y confirma que `spring.datasource.url` apunte a la misma base PostgreSQL donde están esas cuentas. La vista consulta directamente el backend y no copia usuarios desde otro motor.

### Corrección de prioridades de comunicados

La migración Flyway `V8__agregar_prioridad_baja_alertas_publicadas.sql` habilita la prioridad `BAJA` en los comunicados publicados. Al iniciar Spring Boot con Flyway habilitado, la migración se aplica automáticamente. No elimina alertas ni usuarios existentes. Si Flyway está deshabilitado en tu entorno, revisa la configuración antes de iniciar; no ejecutes scripts de recreación de tablas.
