# Frontend SMCV — versión conectable a API

Frontend React + Vite basado en el diseño recibido, con pantalla de bienvenida, inicio de sesión JWT y llamadas al backend Spring Boot proporcionado.

## Requisitos
- Node.js y npm instalados.
- Backend `ptar-backend` ejecutándose y accesible desde el navegador.
- Base de datos configurada por el backend.

## Ejecutar en Windows
1. Descomprime el ZIP.
2. Abre una terminal dentro de `frontend-smcv` (la carpeta donde está `package.json`).
3. Ejecuta `npm.cmd install`.
4. Copia `.env.example` a `.env.local` si necesitas configurar la URL. El valor por defecto es `http://localhost:8080`.
5. Ejecuta `npm.cmd run dev` y abre la URL que muestre Vite, normalmente `http://localhost:5173`.
6. Para comprobar compilación y estilo ejecuta `npm.cmd run build` y `npm.cmd run lint`.

## Inicio de sesión
El inicio de sesión llama a `POST /api/auth/login` con `{ "correo": "...", "password": "..." }` y guarda el JWT que devuelve la API. La sesión se valida de nuevo mediante `GET /api/auth/me`. Los permisos definitivos los controla el backend.

En la guía del backend suministrado, las credenciales de desarrollo del administrador son `admin@smcv.local` / `Admin12345!`. El usuario `admin.admin.p` con contraseña `admin123` no aparece en los archivos entregados. Como esta entrega modifica solo React y no altera la base de datos, no se crea una cuenta ficticia ni se omite la validación del servidor.

## Registro de usuarios
La API suministrada no documenta un registro público. La pantalla «Registrarse» explica que las cuentas deben ser creadas por un administrador mediante la operación protegida existente.

## Persistencia
El formulario de mediciones usa los puntos y parámetros devueltos por la API y envía `POST /api/monitoreos`. Solo muestra confirmación después de una respuesta satisfactoria. La base de datos no se modifica desde el navegador ni se conecta React directamente a MySQL.

Los datos de estaciones, alertas e historial que se reciben después del inicio de sesión se consultan al backend. Los datos de demostración originales del prototipo siguen presentes como estado inicial del código, pero no se usan como credenciales y no se consideran registros oficiales. Para una puesta en producción hay que completar la adaptación de todas las pantallas de edición, seguimiento de alertas, reportes, auditoría y mapa a sus endpoints específicos; no todas esas operaciones están integradas en esta versión.
