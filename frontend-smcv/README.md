# Frontend SMCV — aplicación React conectada a la API

Frontend React + Vite del sistema de monitoreo hídrico de la PTAR La Enlozada. La aplicación consume la API REST del backend Spring Boot; React no se conecta directamente al motor PostgreSQL.

## Requisitos

- Node.js y npm.
- Backend `ptar-backend` ejecutándose y accesible desde el navegador.
- PostgreSQL configurado por el backend.

## Ejecutar en Windows

1. Descomprime el ZIP.
2. Abre una terminal dentro de `frontend-smcv` (donde está `package.json`).
3. Ejecuta `npm.cmd install`.
4. Copia `.env.example` como `.env.local`. La URL predeterminada es `http://localhost:8081`; si cambia, modifica `VITE_API_URL` y reinicia Vite.
5. Ejecuta `npm.cmd run dev` y abre la URL que muestre Vite, normalmente `http://localhost:5173`.
6. Para comprobar compilación y estilo ejecuta `npm.cmd run build` y `npm.cmd run lint`.

## Inicio de sesión

El login llama a `POST /api/auth/login` con correo y contraseña. La sesión se consulta mediante `GET /api/auth/me`; el backend controla los permisos por rol.

En el script inicial `V2__datos_iniciales.sql` se define el usuario administrador `admin@smcv.local`. La contraseña inicial documentada en `ptar-backend/README.md` debe cambiarse después del primer ingreso.

## Registro y recuperación de contraseña

El registro utiliza `POST /api/auth/register`; el backend crea la cuenta con el rol público `AUDITOR` y sin solicitar códigos de verificación.

La recuperación pide solo el correo y utiliza `POST /api/auth/password/solicitar-recuperacion` para enviar un enlace seguro de un solo uso. Al abrirlo, se define la nueva contraseña y se confirma en pantalla mediante `POST /api/auth/password/confirmar-recuperacion`. No hay PIN ni dígitos que el usuario deba copiar. El envío requiere que el SMTP esté configurado en el backend; consulta el apartado de recuperación de contraseña del README del backend.

## Gestión de usuarios y alertas

La sección **Configuración** aparece únicamente para ADMINISTRADOR y consulta `GET /api/usuarios/todos`; muestra cuentas activas e inactivas, administradores y usuarios, con acciones para editar datos, activar/desactivar cuentas y restablecer contraseñas. El contador rojo de avisos publicados no vistos aparece tanto para administrador como para cada usuario, solo cuando esa cuenta tiene avisos pendientes. Cada cuenta puede marcar los avisos como vistos y el cambio se guarda por separado. Los eventos automáticos por umbrales se muestran aparte y no activan el contador rojo.

## Validaciones y persistencia

- Los formularios dentro de la aplicación utilizan validación común para campos obligatorios, campos con solo espacios, restricciones HTML de formato/longitud/rango y confirmación de contraseñas donde existe el campo correspondiente.
- La API vuelve a validar los datos en el servidor. La validación del navegador no sustituye las restricciones del backend.
- En **Estaciones**, el administrador puede agregar, editar y activar/desactivar estaciones. En **Historial de lecturas**, puede agregar y editar mediciones; **Actualizar todo** vuelve a consultar estaciones, lecturas, resumen y eventos.
- El formulario de monitoreo envía lecturas al endpoint `/api/monitoreos` y solo muestra confirmación cuando el backend responde satisfactoriamente.
- Algunas funciones de prototipo mantienen estados locales o datos iniciales de demostración. No todos los módulos de edición, mapa, seguimiento, reportes y auditoría están conectados a endpoints de persistencia en esta versión; revisa `ptar-backend/docs/GUIA_FRONTEND.md` para el contrato disponible.


### Exportaciones

El centro de reportes permite exportar PDF, Excel (.xlsx), PowerPoint (.pptx), PNG y CSV. Para Canva, descarga el PPTX y usa la opción de importar/subir archivo en https://www.canva.com/.
