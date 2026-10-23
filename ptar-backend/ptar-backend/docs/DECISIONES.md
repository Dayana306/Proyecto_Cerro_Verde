# Decisiones de diseño y diferencias con el informe

Este documento resume en qué se aparta la implementación del capítulo III del informe y por qué. Sirve para actualizar la tesis (secciones 3.12, Anexo B y Anexo D) y para defender las decisiones.

## 1. Modelo de datos (21 tablas)

| Cambio | Motivo |
|---|---|
| Nueva tabla **`umbral`** (límites por parámetro y, opcionalmente, por punto, con dos niveles: alerta y crítico) | RF-05 pide umbral "por parámetro y estación". Además separa dos conceptos que el informe mezclaba en `parametro.rango_min/max`: el error de digitación (RF-03) y el incumplimiento normativo (RF-05/06) |
| `parametro.rango_min/max` → **`valor_min_valido/valor_max_valido`** + columna **`codigo`** | Ahora solo representan el rango físicamente posible (RF-03). El código evita depender de nombres con tildes |
| Umbrales **versionados** (cambiar = desactivar el anterior y crear uno nuevo) | Cada alerta conserva el umbral exacto con el que se evaluó; trazabilidad ante ANA/OEFA |
| `alerta`: se agregan **`nivel`**, `id_lectura` e `id_umbral` | El diccionario mencionaba `nivel` pero la tabla no lo tenía. La alerta debe apuntar a la lectura concreta que la originó |
| Nueva tabla **`convenio`** (meta 1.5 m³/s, punto de medición, vigencia) | CU10 exige verificar el convenio y no había dónde guardar la meta |
| `zona`: **`tipo_zona`** y **`poligono_geojson`** | RF-08 y el diccionario hablan de polígonos GeoJSON, pero ninguna tabla los tenía |
| `monitoreo`: FK a `punto_monitoreo`, `fecha_registro` y `estado` | El DDL no tenía la FK; se distingue cuándo se midió de cuándo se registró; se habilita la anulación sin borrar |
| `lectura_parametro`: único (`id_monitoreo`, `id_parametro`), `BIGINT`, `DECIMAL(10,3)` | Evita lecturas duplicadas; RNF-07 (5 años de series); un solo tipo para todos los parámetros |
| `historial` como **bitácora genérica** (acción, entidad, datos antes/después, IP) | RNF-04 exige auditar todas las operaciones, no solo monitoreos y alertas |
| `reporte`: `formato`, filtros (`fecha_desde`, `fecha_hasta`, `id_punto`) y `tipo_reporte`; **se elimina `reporte_especial`** | `formulario` no tenía significado; `reporte_especial` quedaba redundante con `tipo_reporte = 'ESPECIAL'` |
| `reporte_detalle`: estadísticas por punto y parámetro (n, promedio, mín., máx., alertas) | Un solo `valor` no permitía describir un periodo |
| `notificacion`: `canal` y `fecha_lectura` | Saber qué notificaciones leyó cada usuario |
| Estados como `VARCHAR` en mayúsculas, borrado lógico en todo | Consistencia; el usuario de BD de la aplicación no necesita `DELETE` |

## 2. Reglas de negocio implementadas

- **RF-03 (validación):** cada valor debe estar dentro del rango válido del parámetro; si no, se rechaza todo el registro con el detalle por campo. La hora de muestreo no puede ser futura (tolerancia de 10 minutos).
- **RF-05 (alertas):** al registrar o corregir una medición, cada lectura se compara con el umbral vigente (específico del punto o, si no hay, el general). Fuera del rango de alerta → `MEDIA`; fuera del crítico → `ALTA`. Igualar el límite no genera alerta. Una lectura no genera una segunda alerta mientras la primera siga abierta.
- **Notificaciones:** cada alerta notifica a los usuarios activos con rol GERENTE y ADMINISTRADOR (panel). El correo queda como mejora futura (no figura en los RF).
- **Corrección de mediciones:** se actualiza la misma fila de lectura; si tenía alertas abiertas se deja constancia automática en su seguimiento y se re-evalúa el nuevo valor.
- **Anulación de un monitoreo:** sus alertas abiertas se cierran automáticamente con un seguimiento explicativo.
- **Convenio (CU10):** promedio anual de caudal = **promedio de los promedios diarios** (evita que un día con más mediciones pese más). Cumple si es mayor o igual a la meta. Para el año en curso, es el acumulado a la fecha.
- **Auditoría:** se registra cada creación, modificación, cambio de estado, ingreso (y ingreso fallido), cambio de contraseña, generación de reportes, exportación y subida de archivos, en la misma transacción que la operación.
- **Seguridad:** BCrypt, JWT de 8 horas, permisos por rol en cada endpoint, no se puede desactivar la propia cuenta ni dejar el sistema sin administradores activos.

## 3. Decisiones técnicas

| Decisión | Alternativa descartada | Motivo |
|---|---|---|
| Paquetes por módulo funcional | Por capa técnica | Cada módulo corresponde a un RF; facilita la trazabilidad (sección 3.15) |
| Flyway + `ddl-auto=validate` | Que Hibernate cree las tablas | El esquema es un entregable versionado del responsable de BD; el backend verifica que coincide |
| DTOs (`record`) en todas las respuestas | Devolver entidades JPA | El contrato con el frontend no cambia si cambia la base; no se expone `password_hash` |
| OpenPDF para PDF | iText 7 (propuesto en el informe) | API equivalente con licencia LGPL/MPL; iText 7 es AGPL |
| Archivos en disco, ruta en BD | Archivos en BLOB | Base más liviana y respaldos más simples |
| Datos de demostración generados al arrancar (perfil dev) | Script SQL con datos fijos | Fechas siempre recientes, respetan las reglas de negocio (las alertas se generan de verdad) |

## 4. Pendientes / mejoras futuras

- Envío de notificaciones por correo (requiere servidor SMTP).
- Integración con sensores IoT: el tipo de muestreo `SENSOR_IOT` ya existe; faltaría un usuario técnico y un endpoint con autenticación por API key.
- Valores normativos reales de los umbrales: los cargados en el perfil dev son **de ejemplo** y deben validarse con la Gerencia de Medio Ambiente.
- Recuperación de contraseña por correo (hoy el administrador asigna una nueva).
