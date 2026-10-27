# Conectar PTAR con XAMPP/MariaDB desde IntelliJ (sin CMD)

Esta guía usa la cuenta de base de datos separada `ptar_backend`. No es la cuenta `admin` con la que inicias sesión en la web: son credenciales diferentes.

## 1. No tocar los datos existentes

- Mantén la base `ptar_enlozada`; no la elimines ni la recrees.
- Desde phpMyAdmin, selecciona la base y usa **Exportar** para guardar una copia SQL antes de migrar.
- Inicia MySQL/MariaDB de XAMPP en el puerto `3306`.

## 2. Crear la cuenta desde phpMyAdmin

1. En phpMyAdmin, abre **Cuentas de usuario** → **Agregar cuenta de usuario**.
2. Usuario: `ptar_backend`; host: `localhost`.
3. Establece una contraseña nueva que puedas guardar de forma segura. No hace falta compartirla.
4. Deja los privilegios globales sin marcar.
5. En los privilegios de esa cuenta, otorga permisos necesarios únicamente sobre `ptar_enlozada` para que Flyway pueda crear y actualizar las tablas de la aplicación.

Si phpMyAdmin informa que se guardaron los privilegios pero `SHOW GRANTS` no los refleja, o vuelve a aparecer el error Aria `#1030`, no repitas la operación indefinidamente ni borres archivos de la carpeta `C:\xampp\mysql\data`. Puede existir un problema interno de las tablas de permisos MariaDB que debe revisarse antes de seguir.

## 3. Usar esa misma cuenta en IntelliJ

1. Abre `ptar-backend` como proyecto Maven en IntelliJ con JDK 17.
2. Abre **Run → Edit Configurations** y selecciona la configuración de `PtarApplication`.
3. En **Environment variables**, configura:
   - `DB_URL=jdbc:mysql://localhost:3306/ptar_enlozada?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&characterEncoding=UTF-8`
   - `DB_USER=ptar_backend`
   - `DB_PASSWORD=LA_CONTRASEÑA_QUE_TÚ_ELEGISTE`
   - `PORT=8081`
   - `DEMO_DATA=false`
   - `FLYWAY_ENABLED=true` solo después de confirmar el historial Flyway (ver siguiente apartado).
4. Elimina `FLYWAY_USER` y `FLYWAY_PASSWORD` si existen como variables con valores antiguos; por defecto Flyway hereda el mismo usuario y contraseña de la aplicación.
5. Guarda la configuración. No escribas la contraseña en el chat.

## 4. Revisar migraciones antes de arrancar

En phpMyAdmin, consulta de forma solo lectura:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

El paquete conserva V1, V2 y V3 y agrega V4 para el Centro de alertas (personal operativo, equipos, incidencias y mantenimiento). Si el historial demuestra que las migraciones anteriores están completas y correctas, Flyway debe aplicar V4 al iniciar. Si la tabla de historial no existe, tiene errores o no corresponde a tu esquema real, no fuerces el arranque con Flyway activo: revisa primero el respaldo y la estructura actual.

No ejecutes los SQL manuales de `database/` si Flyway va a ejecutar las mismas migraciones. Los scripts manuales son una alternativa para bases administradas manualmente y solo se deben usar después de verificar qué objetos ya existen.

## 5. Datos de demostración

El paquete trae un cargador opcional de nombres ficticios, equipos y un mantenimiento de ejemplo, claramente marcados como demostración. `DEMO_DATA=false` es lo seguro para datos operativos. En una base exclusivamente de pruebas, se puede activar `DEMO_DATA=true` bajo el perfil `dev`; el cargador no reemplaza tablas y se abstiene si ya hay personal/equipos.

## 6. Recuperación de contraseña

El formulario solicita correo y, luego de verificar un enlace temporal, deja establecer y confirmar una nueva contraseña. Para enviar el enlace real, define en IntelliJ `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true` y `FRONTEND_URL=http://localhost:5173`. Sin SMTP real, la pantalla puede solicitar el enlace, pero no se enviará correo.

El backend escucha en `http://localhost:8081`; salud: `http://localhost:8081/actuator/health`.
