# Guía para el responsable del Frontend

Contrato de la API REST del backend. La documentación interactiva (con "Try it out") está en **Swagger UI**: <http://localhost:8080/swagger-ui.html> mientras el backend corre en perfil `dev`.

## 1. Datos básicos

| Tema | Valor |
|---|---|
| URL base | `http://localhost:8080` (todas las rutas empiezan con `/api`) |
| Formato | JSON (`Content-Type: application/json`), salvo subida de archivos (multipart) y descargas |
| Autenticación | JWT en la cabecera `Authorization: Bearer <token>` |
| CORS | Permitidos `http://localhost:5173` (Vite) y `http://localhost:3000`. Otro origen: variable `CORS_ORIGINS` del backend |
| Fechas | `"2026-10-22"` (fecha) y `"2026-10-22T08:00:00"` (fecha y hora, **sin zona**: siempre hora de Perú) |
| Decimales | números JSON (`7.25`). Hasta 3 decimales |
| Campos nulos | se **omiten** de la respuesta (si un campo no aparece, es `null`) |

### Usuarios de prueba (perfil dev)

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | `admin@smcv.local` | `Admin12345!` |
| SUPERVISOR | `supervisor@smcv.local` | `Demo12345!` |
| GERENTE | `gerente@smcv.local` | `Demo12345!` |
| AUDITOR | `auditor@smcv.local` | `Demo12345!` |

## 2. Autenticación

```http
POST /api/auth/login
{ "correo": "admin@smcv.local", "password": "Admin12345!" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 28800,
  "usuario": { "idUsuario": 1, "nombreCompleto": "Administrador Sistema", "correo": "admin@smcv.local", "rol": "ADMINISTRADOR" }
}
```

- Guarda `token` y `usuario` (por ejemplo en `localStorage`) y envía el token en cada petición.
- El token dura 8 horas. Cuando vence, cualquier petición devuelve **401** → redirige al login.
- Al recargar la app, `GET /api/auth/me` devuelve el usuario de la sesión (o 401 si el token ya no sirve).
- No hay logout en el servidor: cerrar sesión = borrar el token del navegador.
- Cambiar la propia contraseña: `PUT /api/auth/password` con `{ "passwordActual": "...", "passwordNueva": "..." }` → 204.

### Cliente sugerido (axios)

```js
// src/api/cliente.js
import axios from "axios";

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080" });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && !error.config.url.includes("/api/auth/login")) {
      localStorage.clear();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);
```

## 3. Roles y pantallas

El rol viene en `usuario.rol`. Úsalo para mostrar u ocultar menús y botones; el backend igual valida y responde **403** si no corresponde.

| Pantalla | ADMINISTRADOR | SUPERVISOR | GERENTE | AUDITOR |
|---|---|---|---|---|
| Dashboard | Ver | Ver | Ver | Ver |
| Registro de mediciones | Crear / editar / anular | Crear / editar / anular | Ver | Ver |
| Historial de monitoreos | Ver | Ver | Ver | Ver |
| Alertas | Ver + seguimiento | Ver + seguimiento | Ver + seguimiento | Ver |
| Notificaciones (campana) | Sí | Sí | Sí | Sí |
| Reportes | Generar, anular, registrar envío | Ver / descargar | Generar, anular, registrar envío | Generar / descargar |
| Mapa 2D | Ver | Ver | Ver | Ver |
| Convenio (cumplimiento) | Ver + configurar | Ver | Ver | Ver |
| Configuración: puntos, zonas, regiones, áreas, parámetros, umbrales, mapas, entidades | CRUD | Ver | Ver | Ver |
| Usuarios | CRUD | — | — | — |
| Bitácora de auditoría | Ver + exportar | — | — | Ver + exportar |

Las notificaciones de alertas llegan a usuarios **GERENTE** y **ADMINISTRADOR**.

## 4. Formatos comunes

### Errores (siempre igual)

```json
{
  "timestamp": "2026-10-22T10:15:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Hay valores inválidos en las lecturas. Verifique los datos ingresados.",
  "path": "/api/monitoreos",
  "errores": { "lecturas[0].valor": "pH fuera del rango válido [0 - 14] pH" }
}
```

| Código | Significado | Qué mostrar |
|---|---|---|
| 400 | Datos mal formados o campos obligatorios vacíos. `errores` trae el detalle por campo | Marcar los campos del formulario |
| 401 | Sin token, token vencido o login incorrecto | Volver al login / "Credenciales inválidas" |
| 403 | El rol no tiene permiso | "No tiene permisos" |
| 404 | No existe el recurso | |
| 409 | Conflicto con la base (dato duplicado) | |
| 413 | Archivo mayor a 10 MB | |
| 422 | Regla de negocio (correo repetido, valor fuera de rango...). Leer `message` (y `errores` si viene) | Mostrar `message` |
| 500 | Error interno | Mensaje genérico |

### Paginación

Los listados grandes (`usuarios`, `monitoreos`, `alertas`, `reportes`, `notificaciones`, `auditoria`) aceptan `?page=0&size=20&sort=campo,desc` y responden:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 135, "totalPages": 7 }
```

`page` empieza en 0. Los catálogos (puntos, parámetros, zonas...) devuelven un arreglo simple.

### Activar / desactivar (borrado lógico)

Nada se elimina. Todos los catálogos tienen `PATCH /{id}/estado` con `{ "estado": "INACTIVO" }` o `{ "estado": "ACTIVO" }`. Los listados aceptan `?estado=ACTIVO` para los combos.

## 5. Catálogo de endpoints

`Rol` = quién puede llamarlo (además de estar autenticado). "Todos" = cualquier usuario autenticado.

### Autenticación y usuarios

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Login |
| GET | `/api/auth/me` | Todos | Usuario actual |
| PUT | `/api/auth/password` | Todos | Cambiar mi contraseña |
| GET | `/api/usuarios?q=&idRol=&estado=` | ADMIN | Lista paginada. `q` busca en nombre, apellido y correo |
| GET | `/api/usuarios/{id}` | ADMIN | Detalle |
| POST | `/api/usuarios` | ADMIN | Crear |
| PUT | `/api/usuarios/{id}` | ADMIN | Editar (no cambia la contraseña) |
| PATCH | `/api/usuarios/{id}/estado` | ADMIN | Activar/desactivar |
| PUT | `/api/usuarios/{id}/password` | ADMIN | Asignar nueva contraseña `{ "passwordNueva": "..." }` |
| GET | `/api/roles` | ADMIN | Roles para el combo |

```json
// POST /api/usuarios
{ "nombre": "Ana", "apellido": "Torres", "correo": "ana@smcv.local", "usuarioLogin": "atorres",
  "password": "Clave12345", "telefono": "999888777", "idRol": 2, "idArea": 1 }
```

### Ubicación y mapa 2D

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/regiones`, `/api/areas`, `/api/mapas` | Todos | Listas (`?estado=`) |
| GET | `/api/zonas?idRegion=&estado=` | Todos | Zonas con `poligonoGeojson` |
| GET | `/api/puntos-monitoreo?idZona=&idArea=&estado=` | Todos | Puntos de monitoreo |
| GET | `/api/mapas/visor` | Todos | **Todo lo del visor 2D en una llamada** |
| POST / PUT / PATCH estado | mismas rutas | ADMIN | CRUD |

```json
// POST /api/puntos-monitoreo
{ "nombrePunto": "Salida PTAR", "descripcion": "...", "latitud": -16.4262, "longitud": -71.6128, "idZona": 1, "idArea": 2 }

// POST /api/zonas  (poligonoGeojson es un TEXTO con el GeoJSON)
{ "nombreZona": "PTAR", "tipoZona": "COMPONENTE", "idRegion": 1,
  "poligonoGeojson": "{\"type\":\"Polygon\",\"coordinates\":[[[-71.61,-16.42],[-71.60,-16.42],[-71.60,-16.43],[-71.61,-16.42]]]}" }
```

**Visor 2D** (`GET /api/mapas/visor`):

```json
{
  "mapaBase": { "idMapa": 1, "nombreMapa": "Plano general", "imagenRuta": "/mapas/ptar-la-enlozada.png" },
  "zonas":  [ { "idZona": 1, "nombreZona": "PTAR La Enlozada", "tipoZona": "COMPONENTE", "poligonoGeojson": "{...}" } ],
  "puntos": [ { "punto": { "idPunto": 2, "nombrePunto": "Salida PTAR (efluente)", "latitud": -16.4262, "longitud": -71.6128 },
                "alertasAbiertas": 1, "nivelMaximo": "MEDIA" } ]
}
```

- Con **Leaflet** (`react-leaflet`): `JSON.parse(zona.poligonoGeojson)` en `<GeoJSON data={...}/>` y un `<CircleMarker>` por punto. Color sugerido: sin alertas verde, `MEDIA` ámbar, `ALTA` rojo.
- Ojo: GeoJSON usa **[longitud, latitud]**, Leaflet usa `[lat, lng]` en marcadores.
- `imagenRuta` es una ruta del frontend (pon la imagen en `public/mapas/`) o una URL. Es opcional si usas un mapa base de OpenStreetMap.

### Parámetros y umbrales

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/parametros?estado=ACTIVO` | Todos | Para armar el formulario de mediciones |
| POST / PUT / PATCH estado | `/api/parametros...` | ADMIN | CRUD. `valorMinValido`/`valorMaxValido` = rango físicamente posible |
| GET | `/api/umbrales?idParametro=&idPunto=&soloGenerales=false&estado=ACTIVO` | Todos | Umbrales vigentes (`estado=INACTIVO` = historial de versiones) |
| POST | `/api/umbrales` | ADMIN | Crear (reemplaza el vigente del mismo parámetro+punto) |
| PUT | `/api/umbrales/{id}` | ADMIN | Cambiar límites → devuelve la **nueva versión con otro id** |
| PATCH | `/api/umbrales/{id}/estado` | ADMIN | Desactivar |

```json
// POST /api/umbrales   (idPunto null = aplica a todos los puntos)
{ "idParametro": 1, "idPunto": null, "limiteMin": 6.5, "limiteMax": 8.5, "criticoMin": 6.0, "criticoMax": 9.0 }
```

Regla de alertas: fuera de `[limiteMin, limiteMax]` → alerta **MEDIA**; fuera de `[criticoMin, criticoMax]` → **ALTA**. Cualquier límite puede ir vacío. Si un punto tiene umbral propio, se usa ese; si no, el general.

### Monitoreos (registro de mediciones)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/monitoreos?idPunto=&estado=&desde=&hasta=&page=&size=` | Todos | Historial paginado (más reciente primero) |
| GET | `/api/monitoreos/{id}` | Todos | Detalle con lecturas y alertas |
| POST | `/api/monitoreos` | ADMIN, SUPERVISOR | Registrar |
| PUT | `/api/monitoreos/{id}` | ADMIN, SUPERVISOR | Corregir (mismo cuerpo que POST) |
| PATCH | `/api/monitoreos/{id}/estado` | ADMIN, SUPERVISOR | `INACTIVO` = anular (cierra sus alertas) |
| GET | `/api/monitoreos/{id}/documentos` | Todos | Archivos adjuntos |
| POST | `/api/monitoreos/{id}/documentos` | ADMIN, SUPERVISOR | Subir archivo (multipart) |
| GET | `/api/documentos/{id}/descarga` | Todos | Descargar archivo |
| PATCH | `/api/documentos/{id}/estado` | ADMIN, SUPERVISOR | Desactivar archivo |

```json
// POST /api/monitoreos
{
  "idPunto": 2,
  "tipoMuestreo": "MANUAL",
  "horaMuestreo": "2026-10-22T08:00:00",
  "observacion": "Muestreo de rutina",
  "lecturas": [
    { "idParametro": 1, "valor": 7.25 },
    { "idParametro": 2, "valor": 1.62 },
    { "idParametro": 3, "valor": 18.4 },
    { "idParametro": 4, "valor": 21.0 }
  ]
}
```

Respuesta 201: el monitoreo con `lecturas` (cada una con `nivelAlerta` si quedó en alerta) y `alertas` generadas. Muestra un aviso si `alertas` no está vacío. Si un valor está fuera del rango físico, responde **422** con `errores["lecturas[i].valor"]` (todos los errores juntos). La hora no puede ser futura.

**Subir archivo** (pdf, png, jpg, xlsx, xls, docx, doc, csv, txt; máx. 10 MB):

```js
const fd = new FormData();
fd.append("archivo", file);
fd.append("descripcion", "Acta de muestreo");
await api.post(`/api/monitoreos/${id}/documentos`, fd);   // axios pone el Content-Type multipart
```

### Alertas y notificaciones

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/alertas?estado=&nivel=&idPunto=&idParametro=&desde=&hasta=` | Todos | Paginado |
| GET | `/api/alertas/{id}` | Todos | Detalle con `seguimientos` |
| POST | `/api/alertas/{id}/seguimientos` | ADMIN, GERENTE, SUPERVISOR | `{ "comentario": "...", "nuevoEstado": "EN_REVISION" }` (`nuevoEstado` opcional) |
| GET | `/api/notificaciones?soloNoLeidas=true` | Todos | Mis notificaciones (paginado) |
| GET | `/api/notificaciones/no-leidas/total` | Todos | `{ "total": 3 }` → número de la campana |
| PATCH | `/api/notificaciones/{id}/leida` | Todos | Marcar una |
| PATCH | `/api/notificaciones/leidas` | Todos | Marcar todas |

Estados de una alerta: `ABIERTA` → `EN_REVISION` → `CERRADA` (cerrada ya no admite cambios). Las alertas las crea el sistema al registrar mediciones; no hay POST de alertas.

Para la campana basta con consultar `/api/notificaciones/no-leidas/total` cada 30-60 segundos (`setInterval`).

### Dashboard

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/dashboard/resumen?idPunto=` | Tarjetas, conteo de alertas, actividad, convenio y 5 alertas recientes |
| GET | `/api/dashboard/tendencia?idParametro=1&idPunto=&desde=&hasta=` | Serie diaria para gráficos (por defecto últimos 30 días) |

```json
// GET /api/dashboard/resumen  (fragmento)
{
  "indicadores": [
    { "codigo": "PH", "parametro": "pH", "unidadMedida": "pH", "ultimoValor": 7.31,
      "fecha": "2026-10-22T08:00:00", "punto": "Salida PTAR (efluente)", "estado": "NORMAL",
      "limiteMin": 6.5, "limiteMax": 8.5 }
  ],
  "alertas": { "noCerradas": 4, "alta": 1, "media": 3 },
  "monitoreosHoy": 3, "monitoreosUltimos30Dias": 180, "puntosActivos": 3,
  "convenio": { "metaM3s": 1.5, "promedioM3s": 1.612, "porcentajeMeta": 107.5, "cumple": true },
  "alertasRecientes": [ ... ]
}
```

`indicadores[].estado`: `NORMAL` (verde), `MEDIA` (ámbar), `ALTA` (rojo), `SIN_UMBRAL` (gris), `SIN_DATOS`.

`tendencia` devuelve `dias: [{ fecha, promedio, minimo, maximo, mediciones }]` y `umbral: { limiteMin, limiteMax, criticoMin, criticoMax }` para dibujar líneas de referencia (Recharts: `<LineChart>` + `<ReferenceLine y={umbral.limiteMax}/>`).

### Convenio y entidades externas

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/convenios` | Todos | Convenios |
| GET | `/api/convenios/{id}/cumplimiento?anio=2026` | Todos | Promedio anual vs. meta, con detalle mensual |
| POST / PUT / PATCH estado | `/api/convenios...` | ADMIN | CRUD |
| GET | `/api/entidades-externas` | Todos | SEDAPAR, ANA, OEFA... |
| POST / PUT / PATCH estado | `/api/entidades-externas...` | ADMIN | CRUD |

### Reportes

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/reportes?tipo=&estado=` | Todos | Historial paginado |
| GET | `/api/reportes/{id}` | Todos | Detalle con estadísticas (`detalles`) y `envios` |
| POST | `/api/reportes` | ADMIN, GERENTE, AUDITOR | Generar |
| GET | `/api/reportes/{id}/descarga` | Todos | Descargar el PDF/Excel |
| PATCH | `/api/reportes/{id}/anular` | ADMIN, GERENTE | Anular |
| GET / POST | `/api/reportes/{id}/envios` | Todos / ADMIN, GERENTE | Ver / registrar entrega a una entidad |

```json
// POST /api/reportes
{ "tipoReporte": "AMBIENTAL", "formato": "PDF", "fechaDesde": "2026-10-01", "fechaHasta": "2026-10-22",
  "idPunto": null, "descripcion": "Informe mensual" }

// POST /api/reportes/{id}/envios
{ "idEntidad": 2, "medio": "MESA_DE_PARTES", "fechaEnvio": "2026-10-22T11:00:00", "observacion": "Expediente 123" }
```

Tipos: `AMBIENTAL`, `AUDITORIA` (incluye la bitácora), `ESPECIAL`. Formatos: `PDF`, `XLSX`. Periodo máximo: 366 días.

### Auditoría

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/auditoria?entidad=&accion=&idUsuario=&desde=&hasta=` | ADMIN, AUDITOR | Bitácora paginada |
| GET | `/api/auditoria/exportar?desde=&hasta=` | ADMIN, AUDITOR | Excel con la bitácora |

## 6. Descargar archivos (PDF, Excel, documentos)

Las descargas requieren el token, así que **no sirve un `<a href>` directo**. Pídelas como blob:

```js
export async function descargar(url) {
  const r = await api.get(url, { responseType: "blob" });
  const dispo = r.headers["content-disposition"] ?? "";
  const nombre = decodeURIComponent((dispo.match(/filename\*=UTF-8''([^;]+)/) ?? [])[1] ?? "archivo");
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(r.data);
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(enlace.href);
}

// uso: descargar(`/api/reportes/${id}/descarga`)  o  descargar(reporte.urlDescarga)
```

Los objetos de reportes y documentos traen `urlDescarga` listo para usar.

## 7. Valores fijos (enums) para combos

| Campo | Valores |
|---|---|
| `estado` (general) | `ACTIVO`, `INACTIVO` |
| `tipoZona` | `POBLADO`, `COMPONENTE`, `AREA_INFLUENCIA` |
| `tipoMuestreo` | `MANUAL`, `SEMIAUTOMATICO`, `SENSOR_IOT` |
| `nivel` (alerta) | `MEDIA`, `ALTA` |
| `tipoAlerta` | `MAXIMO_EXCEDIDO`, `MINIMO_EXCEDIDO` |
| `estado` (alerta) | `ABIERTA`, `EN_REVISION`, `CERRADA` |
| `tipoEntidad` | `REGULADOR`, `FISCALIZADOR`, `EMPRESA_SANEAMIENTO`, `OTRO` |
| `tipoReporte` | `AMBIENTAL`, `AUDITORIA`, `ESPECIAL` |
| `formato` | `PDF`, `XLSX` |
| `medio` (envío) | `CORREO`, `MESA_DE_PARTES`, `PLATAFORMA_VIRTUAL`, `OTRO` |
| `accion` (auditoría) | `CREAR`, `MODIFICAR`, `CAMBIAR_ESTADO`, `LOGIN`, `LOGIN_FALLIDO`, `CAMBIAR_PASSWORD`, `GENERAR`, `EXPORTAR`, `SUBIR_ARCHIVO` |

## 8. Cómo levantar el backend para desarrollar el frontend

Lo más simple, con Docker instalado, desde la carpeta del backend:

```bash
docker compose up -d --build
```

En 1-2 minutos la API está en <http://localhost:8080> con datos de demostración. Comprueba <http://localhost:8080/actuator/health> → `{"status":"UP"}`. Para apagarlo: `docker compose down`.
