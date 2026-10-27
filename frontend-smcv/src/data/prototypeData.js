// Datos que ya estaban en el prototipo recibido. Son DEMOSTRATIVOS y deben validarse.
// No son una fuente oficial ni implican ubicaciones geográficas reales.
export const stations = [
  { id: 'P01', name: 'Bocatoma', area: 'Captación', ph: 7.1, turbidity: 9.8, flow: 1.52, temperature: 17.8, status: 'Observado', alerts: 1 },
  { id: 'P02', name: 'Río principal', area: 'Cauce principal', ph: 7.2, turbidity: 14.6, flow: 1.49, temperature: 18.2, status: 'Observado', alerts: 3 },
  { id: 'P03', name: 'Salida PTAR', area: 'Tratamiento', ph: 6.9, turbidity: 8.1, flow: 1.31, temperature: 18.5, status: 'Conforme', alerts: 0 },
  { id: 'P04', name: 'Canal de derivación', area: 'Distribución', ph: 7.0, turbidity: 12.0, flow: 1.40, temperature: 18.1, status: 'Conforme', alerts: 1 },
  { id: 'P05', name: 'Punto de reúso', area: 'Reúso', ph: 7.05, turbidity: 10.2, flow: 1.46, temperature: 17.9, status: 'Conforme', alerts: 1 },
];

export const initialRecords = [
  { id: 'REG-0082', station: 'P02 · Río principal', date: '26/08/2026', time: '10:30', ph: 7.2, turbidity: 14.6, temperature: 18.2, flow: 1.49, observation: 'Control rutinario' },
  { id: 'REG-0081', station: 'P01 · Bocatoma', date: '26/08/2026', time: '09:45', ph: 7.1, turbidity: 9.8, temperature: 17.8, flow: 1.52, observation: 'Revisión de caudal' },
  { id: 'REG-0080', station: 'P03 · Salida PTAR', date: '25/08/2026', time: '16:20', ph: 6.9, turbidity: 8.1, temperature: 18.5, flow: 1.31, observation: 'Muestra de rutina' },
];

export const initialAlerts = [
  { id: 'ALT-026', level: 'Alta', title: 'Turbidez por encima del umbral', station: 'P02 · Río principal', value: '32 NTU', limit: '25 NTU', time: '26 ago. 2026 · 10:30', status: 'Notificada' },
  { id: 'ALT-025', level: 'Media', title: 'pH fuera del rango recomendado', station: 'P03 · Salida PTAR', value: '5.6 pH', limit: '6.0 – 9.0', time: '26 ago. 2026 · 09:15', status: 'En revisión' },
  { id: 'ALT-024', level: 'Alta', title: 'Caudal excede el límite de referencia', station: 'P01 · Bocatoma', value: '1.58 m³/s', limit: '1.50 m³/s', time: '25 ago. 2026 · 22:05', status: 'Escalada' },
];

export const initialAudit = [
  { date: '26/08/2026 10:30', event: 'Alerta generada', station: 'P02', origin: 'Sistema automático', detail: 'Turbidez 32 > 25 NTU', status: 'Notificada', kind: 'danger' },
  { date: '25/08/2026 22:05', event: 'Alerta escalada', station: 'P01', origin: 'Sistema automático', detail: 'Caudal 1.58 > 1.50 m³/s', status: 'Escalada', kind: 'danger' },
  { date: '25/08/2026 09:00', event: 'Reporte generado', station: 'Todas', origin: 'Gerencia ambiental', detail: 'Reporte RA-2026-0041 generado', status: 'Registrado', kind: 'success' },
  { date: '24/08/2026 14:12', event: 'Umbral modificado', station: 'P03', origin: 'Administrador', detail: 'Turbidez máxima actualizada', status: 'Auditado', kind: 'warning' },
];

export const navigation = [
  { id: 'dashboard', label: 'Resumen general', icon: 'dashboard', roles: ['Administrador', 'Usuario normal'] },
  { id: 'monitoreo', label: 'Registro de monitoreo', icon: 'activity', roles: ['Administrador'] },
  { id: 'estaciones', label: 'Estaciones', icon: 'pin', roles: ['Administrador', 'Usuario normal'] },
  { id: 'mapa2d', label: 'Mapa 2D', icon: 'map', roles: ['Administrador', 'Usuario normal'] },
  { id: 'alertas', label: 'Alertas', icon: 'bell', roles: ['Administrador', 'Usuario normal'] },
  { id: 'reportes', label: 'Reportes y auditoría', icon: 'file', roles: ['Administrador', 'Usuario normal'] },
  { id: 'usuarios', label: 'Configuración', icon: 'settings', roles: ['Administrador'] },
  { id: 'configuracion', label: 'Mi perfil', icon: 'user', roles: ['Administrador', 'Usuario normal'] },
];
