# Configurar correo SMTP para PTAR

El proyecto usa **Jakarta Mail 2.0.1** (dependencia Maven `com.sun.mail:jakarta.mail`) para enviar correo desde:

- Recuperación de contraseña (código de un solo uso).
- Módulo **Notificaciones**, con texto plano, HTML y adjuntos.

## 1. Configuración Gmail de prueba

1. Usa una cuenta de Gmail dedicada para el proyecto, no una cuenta personal con información sensible.
2. Activa la verificación en dos pasos de esa cuenta.
3. Si Gmail permite la opción, crea una contraseña de aplicación y cópiala al campo `MAIL_PASSWORD`. No escribas la contraseña normal de Gmail.
4. No habilites "acceso de aplicaciones poco seguras". Ese paso aparece en una diapositiva antigua (2021) y no es el método actual recomendado por Google.

Variables del backend (IntelliJ: **Run > Edit Configurations > Environment variables**):

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-cuenta@gmail.com
MAIL_PASSWORD=CLAVE_DE_APLICACION_DE_16_CARACTERES
MAIL_FROM=tu-cuenta@gmail.com
MAIL_AUTH=true
MAIL_STARTTLS=true
MAIL_SSL=false
```

Para el puerto 587 se usa STARTTLS; no se debe activar SSL implícito al mismo tiempo. Si tu proveedor requiere el puerto 465, configura `MAIL_PORT=465`, `MAIL_STARTTLS=false` y `MAIL_SSL=true`.

## 2. Ejecutar

1. Configura las variables en IntelliJ y reinicia `PtarApplication`.
2. Entra a PTAR como **Administrador**.
3. Abre el menú **Notificaciones**.
4. Comprueba que el estado SMTP indique **Configurado**.
5. Envía una notificación primero a tu propio correo. Prueba texto y después un PDF pequeño como adjunto.
6. Revisa la bandeja de entrada y correo no deseado. El historial del módulo muestra `ENVIADO` o `ERROR`.

La misma configuración SMTP habilita el correo de recuperación de contraseña.

## 3. Límites

- Hasta 30 destinatarios por mensaje.
- Hasta 5 archivos adjuntos por mensaje.
- Máximo 10 MB por archivo.
- Asunto: 180 caracteres como máximo.
- Mensaje: 20 000 caracteres como máximo.
- El módulo de envío está limitado al rol Administrador.

## 4. Errores habituales

- **SMTP no configurado:** faltan `MAIL_HOST`, `MAIL_FROM` o credenciales si la autenticación está habilitada.
- **535 / Authentication failed:** revisa usuario y contraseña de aplicación; no uses la contraseña normal si el proveedor requiere contraseña de aplicación.
- **Connection timed out / Connection refused:** revisa host, puerto, acceso a Internet y si la red bloquea SMTP.
- **Sender address rejected:** `MAIL_FROM` debe ser una dirección permitida por el proveedor y normalmente debe coincidir con la cuenta autenticada.
- **El mensaje aparece como ERROR:** abre la fila del historial para leer el resumen; la traza técnica completa se escribe en la consola de IntelliJ.

## 5. Sobre el material S16

La implementación conserva el enfoque mostrado en las diapositivas: dependencia Maven, `Session`, `MimeMessage`, `Transport.sendMessage`, texto/HTML y `MimeMultipart`/`MimeBodyPart` para adjuntos. Se ajustaron los parámetros SMTP para separar STARTTLS (587) de SSL implícito (465) y para no incluir credenciales en el código fuente.

Referencia oficial de Google sobre contraseñas de aplicación: https://support.google.com/mail/answer/185833
