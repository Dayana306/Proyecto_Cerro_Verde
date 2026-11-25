import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Icon from './components/Icon.jsx';
import { EmptyState, MetricCard, Pill, SectionTitle } from './components/Shared.jsx';
import Map2D from './features/Map2D.jsx';
import MonitoringEquipmentRegister from './features/MonitoringEquipmentRegister.jsx';
import ExportCenter from './features/ExportCenter.jsx';
import NotificationCenter from './features/NotificationCenter.jsx';
import OperationsPanel from './features/OperationsPanel.jsx';
import StationAndReadings from './features/StationAndReadings.jsx';
import PublishedAlerts from './features/PublishedAlerts.jsx';
import UserNotifications from './features/UserNotifications.jsx';
import AccountSettings from './features/AccountSettings.jsx';
import UserManagement from './features/UserManagement.jsx';
import { navigation } from './data/prototypeData.js';
import { apiRequest, clearSession, currentUser, login as apiLogin, registerAccount } from './api.js';
import { validateFormFields } from './utils/formValidation.js';

function todayDisplay() {
  return new Date().toLocaleDateString('es-PE');
}

function toUiAlert(alert) {
  const level = String(alert.nivel || 'MEDIA').toUpperCase();
  const limit = alert.umbral?.limiteMax ?? alert.umbral?.limiteMin ?? alert.umbral?.criticoMax ?? alert.umbral?.criticoMin;
  return {
    id: String(alert.idAlerta ?? alert.id ?? ''),
    level: level.includes('ALTA') || level.includes('CRIT') ? 'Alta' : level.includes('BAJA') ? 'Baja' : 'Media',
    title: alert.parametro ? `${alert.parametro}: revisar umbral` : (alert.tipoAlerta || 'Alerta de monitoreo'),
    station: alert.punto || alert.nombrePunto || 'Punto de monitoreo',
    value: alert.valorDetectado != null ? String(alert.valorDetectado) : '—',
    limit: limit != null ? String(limit) : '—',
    time: alert.horaMuestreo || alert.fechaDeteccion || '',
    status: alert.estado || 'ABIERTA',
  };
}

function toUiRecord(record = {}) {
  // El contrato actual de Spring devuelve `lecturas` con `codigo`, `parametro` y `valor`.
  // Se aceptan también nombres anidados para no perder datos si una versión de API los serializa así.
  const readings = Array.isArray(record.lecturas) ? record.lecturas
    : Array.isArray(record.lecturasParametro) ? record.lecturasParametro
      : Array.isArray(record.valores) ? record.valores : [];
  const valueFor = (kind) => {
    const normalize = (value) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toUpperCase();
    const match = readings.find((reading) => {
      const nestedParameter = reading.parametro && typeof reading.parametro === 'object' ? reading.parametro : {};
      const code = normalize(reading.codigo || reading.codigoParametro || nestedParameter.codigo || reading.parametroCodigo);
      const rawName = typeof reading.parametro === 'string' ? reading.parametro : '';
      const name = normalize(rawName || reading.nombreParametro || reading.nombre || nestedParameter.nombreParametro || nestedParameter.nombre || reading.parametroNombre);
      if (kind === 'ph') return code === 'PH' || name === 'PH' || name.includes('HIDROGENO') || /\bPH\b/.test(name);
      if (kind === 'turbidity') return code.includes('TURB') || name.includes('TURBIDEZ');
      if (kind === 'temperature') return code.includes('TEMP') || name.includes('TEMPERATURA');
      if (kind === 'flow') return code.includes('CAUD') || code.includes('FLU') || name.includes('CAUDAL') || name.includes('FLUJO');
      return false;
    });
    return match?.valor ?? match?.value ?? '—';
  };
  const timestamp = record.horaMuestreo || record.fechaHora || (record.fecha && record.hora ? `${record.fecha}T${record.hora}` : record.fecha);
  const date = timestamp ? new Date(timestamp) : null;
  const validDate = date && !Number.isNaN(date.getTime());
  const station = record.punto || record.nombrePunto || record.estacion?.nombrePunto || record.estacion?.nombre || 'Punto de monitoreo';
  return {
    id: String(record.idMonitoreo ?? record.id ?? ''),
    station,
    date: validDate ? date.toLocaleDateString('es-PE') : '',
    time: validDate ? date.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '',
    ph: valueFor('ph'),
    turbidity: valueFor('turbidity'),
    temperature: valueFor('temperature'),
    flow: valueFor('flow'),
    observation: record.observacion || record.observation || '',
  };
}

function responseRows(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
}

function hasRowsEnvelope(payload) {
  return Array.isArray(payload) || Array.isArray(payload?.content)
    || Array.isArray(payload?.data?.content) || Array.isArray(payload?.items)
    || Array.isArray(payload?.data);
}

async function fetchAllAlertas() {
  const pageSize = 100;
  const allRows = [];
  let page = 0;
  let finished = false;
  while (!finished && page < 1000) {
    const payload = await apiRequest(`/api/alertas?page=${page}&size=${pageSize}&sort=fechaDeteccion,desc`);
    if (!hasRowsEnvelope(payload)) throw new Error('No se pudieron consultar los eventos automáticos.');
    const rows = responseRows(payload);
    allRows.push(...rows);
    const rawTotalPages = payload?.totalPages ?? payload?.data?.totalPages;
    const totalPages = rawTotalPages == null ? null : Number(rawTotalPages);
    finished = totalPages != null && Number.isInteger(totalPages) && totalPages >= 0
      ? totalPages === 0 || page + 1 >= totalPages
      : rows.length < pageSize;
    page += 1;
  }
  if (!finished) throw new Error('No fue posible completar la consulta de eventos automáticos.');
  const uniqueRows = new Map();
  allRows.forEach((row) => {
    const id = row?.idAlerta ?? row?.id;
    if (id != null) uniqueRows.set(String(id), row);
  });
  return [...uniqueRows.values()];
}

async function fetchAllMonitoreos() {
  const payload = await apiRequest('/api/monitoreos/recientes?limite=5000');
  if (!hasRowsEnvelope(payload)) {
    throw new Error('No se pudo consultar el historial de lecturas. Actualiza la pantalla e inténtalo nuevamente.');
  }
  const rows = responseRows(payload);
  const uniqueRows = new Map();
  rows.forEach((row) => {
    const id = row?.idMonitoreo ?? row?.id;
    if (id != null) uniqueRows.set(String(id), row);
  });
  return [...uniqueRows.values()].sort((a, b) => {
    const dateA = new Date(a.horaMuestreo || a.fechaHora || 0).getTime();
    const dateB = new Date(b.horaMuestreo || b.fechaHora || 0).getTime();
    return dateB - dateA || Number(b.idMonitoreo || b.id || 0) - Number(a.idMonitoreo || a.id || 0);
  });
}

function TrendChart({ data, unit }) {
  const values = (data?.dias || []).filter((d) => d.promedio != null);
  if (!values.length) return <div className="empty-state trend-empty"><Icon name="activity" size={24}/><strong>Sin mediciones en el periodo</strong><span>El gráfico se actualizará cuando existan lecturas guardadas para este parámetro.</span></div>;
  const w = 720, h = 220, pad = { l: 42, r: 18, t: 18, b: 34 };
  const nums = values.map((d) => Number(d.promedio));
  let min = Math.min(...nums), max = Math.max(...nums);
  if (min === max) { min -= 1; max += 1; }
  const x = (i) => pad.l + (values.length < 2 ? (w - pad.l - pad.r) / 2 : i * (w - pad.l - pad.r) / (values.length - 1));
  const y = (v) => pad.t + (max - v) * (h - pad.t - pad.b) / (max - min);
  const points = values.map((d, i) => `${x(i)},${y(Number(d.promedio))}`).join(' ');
  const dateLabel = (v) => new Date(`${v}T12:00:00`).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit' });
  return <div className="trend-chart-wrap"><div className="trend-chart-meta"><span>{values.length} días con mediciones</span><span>Unidad: {unit || '—'}</span></div><svg className="trend-chart" viewBox={`0 0 ${w} ${h}`} role="img" aria-label="Tendencia de lecturas registradas"><line x1={pad.l} y1={pad.t} x2={pad.l} y2={h-pad.b} className="chart-axis"/><line x1={pad.l} y1={h-pad.b} x2={w-pad.r} y2={h-pad.b} className="chart-axis"/>{[0,1,2,3].map((i)=><g key={i}><line x1={pad.l} y1={pad.t+i*(h-pad.t-pad.b)/3} x2={w-pad.r} y2={pad.t+i*(h-pad.t-pad.b)/3} className="chart-grid"/><text x={pad.l-8} y={pad.t+i*(h-pad.t-pad.b)/3+4} textAnchor="end" className="chart-label">{(max-i*(max-min)/3).toLocaleString('es-PE',{maximumFractionDigits:2})}</text></g>)}<polyline points={points} className="chart-line" fill="none" strokeLinecap="round" strokeLinejoin="round"/>{values.map((d,i)=><g key={d.fecha}><circle cx={x(i)} cy={y(Number(d.promedio))} r="3.5" className="chart-point"><title>{`${d.fecha}: ${d.promedio} ${unit || ''} (${d.mediciones} lecturas)`}</title></circle>{(values.length<=10 || i===0 || i===values.length-1 || i%Math.ceil(values.length/6)===0)&&<text x={x(i)} y={h-12} textAnchor="middle" className="chart-label">{dateLabel(d.fecha)}</text>}</g>)}</svg><div className="trend-chart-foot">Valores promedio diarios de las mediciones registradas. No se completan días sin mediciones con datos ficticios.</div></div>;
}

function registerFieldMessage(field, form) {
  const value = String(form[field] ?? '');
  switch (field) {
    case 'nombre': return value.trim() ? '' : 'Ingresa tus nombres.';
    case 'apellido': return value.trim() ? '' : 'Ingresa tus apellidos.';
    case 'correo': return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? '' : 'Ingresa un correo electrónico válido.';
    case 'usuarioLogin': return value.trim().length >= 3 && value.trim().length <= 40 ? '' : 'El usuario debe tener entre 3 y 40 caracteres.';
    case 'telefono': return !value || /^9\d{8}$/.test(value) ? '' : 'El celular debe tener 9 dígitos y comenzar con 9.';
    case 'password': return value.length >= 8 && value.length <= 72 ? '' : 'La contraseña debe tener entre 8 y 72 caracteres.';
    case 'confirmarPassword': return value && value === form.password ? '' : 'Las contraseñas no coinciden.';
    default: return '';
  }
}

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [role, setRole] = useState('Usuario normal');
  const [displayName, setDisplayName] = useState('');
  const [sessionUser, setSessionUser] = useState(null);
  const [profilePhoto, setProfilePhoto] = useState('');
  const [dashboardSummary, setDashboardSummary] = useState(null);
  const [dashboardParams, setDashboardParams] = useState([]);
  const [trendParameter, setTrendParameter] = useState('');
  const [trendData, setTrendData] = useState(null);
  const [trendError, setTrendError] = useState('');
  const [remotePoints, setRemotePoints] = useState([]);
  const [entryScreen, setEntryScreen] = useState('welcome');
  const [recoveryCodeSent, setRecoveryCodeSent] = useState(false);
  const [resetForm, setResetForm] = useState({ codigo: '', nueva: '', confirmar: '' });
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [registerForm, setRegisterForm] = useState({ nombre: '', apellido: '', correo: '', usuarioLogin: '', password: '', confirmarPassword: '', telefono: '' });
  const [registerTouched, setRegisterTouched] = useState({});
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');
  const [authNotice, setAuthNotice] = useState('');
  const [mobileNav, setMobileNav] = useState(false);
  const [view, setView] = useState('dashboard');
  const [openAddReading, setOpenAddReading] = useState(false);
  const [publishedUnreadCount, setPublishedUnreadCount] = useState(0);
  const [personalNotificationCount, setPersonalNotificationCount] = useState(0);
  const [publishedAlerts, setPublishedAlerts] = useState([]);
  // Solo se muestran registros y alertas confirmados por la API; no se cargan filas demostrativas como si fueran reales.
  const [alerts, setAlerts] = useState([]);
  const [records, setRecords] = useState([]);
  const [recordsLoadError, setRecordsLoadError] = useState('');
  const [reports, setReports] = useState([]);
  const [audit, setAudit] = useState([]);
  const [toast, setToast] = useState('');
  const toastTimer = useRef(null);
  const [editor, setEditor] = useState(null);
  const [editorForm, setEditorForm] = useState({});
  const [editorBusy, setEditorBusy] = useState(false);
  const roleLabel = (apiRole) => ({ ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', GERENTE: 'Gerente', AUDITOR: 'Auditor' }[apiRole] || 'Usuario');
  const roleForNavigation = (apiRole) => String(apiRole || '').trim().toUpperCase() === 'ADMINISTRADOR' ? 'Administrador' : 'Usuario normal';
  const registerFieldError = (field) => registerTouched[field] ? registerFieldMessage(field, registerForm) : '';
  const updateRegisterField = (field, value) => setRegisterForm((previous) => ({ ...previous, [field]: value }));
  const touchRegisterField = (field) => setRegisterTouched((previous) => ({ ...previous, [field]: true }));

  useEffect(() => {
    const restore = async () => {
      if (!localStorage.getItem('smcv_token')) return;
      try {
        const user = await currentUser();
        setSessionUser(user);
        setDisplayName(user.nombreCompleto || user.correo || '');
        setRole(roleForNavigation(user.rol));
        setAuthenticated(true);
        const [pointsResult, alertsResult, recordsResult] = await Promise.allSettled([
          apiRequest(user.rol === 'ADMINISTRADOR' ? '/api/puntos-monitoreo' : '/api/puntos-monitoreo?estado=ACTIVO'),
          fetchAllAlertas(),
          fetchAllMonitoreos(),
        ]);
        if (pointsResult.status === 'fulfilled') setRemotePoints(responseRows(pointsResult.value));
        if (alertsResult.status === 'fulfilled') setAlerts(responseRows(alertsResult.value).map(toUiAlert));
        if (recordsResult.status === 'fulfilled') {
          const list = responseRows(recordsResult.value);
          setRecords(list.map(toUiRecord));
          setRecordsLoadError('');
        } else {
          setRecordsLoadError(recordsResult.reason?.message || 'No se pudo consultar el historial de lecturas.');
        }
        try { const notices = await apiRequest('/api/alertas-publicadas'); if (Array.isArray(notices)) { setPublishedAlerts(notices); setPublishedUnreadCount(notices.filter((item) => !item.leida).length); } } catch { setPublishedAlerts([]); setPublishedUnreadCount(0); }
      } catch { clearSession(); }
    };
    restore();
    const expire = () => { setAuthenticated(false); setSessionUser(null); setRecords([]); setRecordsLoadError(''); setPublishedAlerts([]); setPublishedUnreadCount(0); setPersonalNotificationCount(0); setEntryScreen('login'); setAuthError('Tu sesión ha vencido. Inicia sesión nuevamente.'); };
    window.addEventListener('smcv:session-expired', expire);
    return () => window.removeEventListener('smcv:session-expired', expire);
  }, []);

  useEffect(() => {
    if (!authenticated) return;
    let cancelled = false;
    Promise.allSettled([
      apiRequest('/api/dashboard/resumen'),
      apiRequest('/api/parametros?estado=ACTIVO'),
      apiRequest('/api/perfil'),
    ]).then(([summaryResult, paramsResult, profileResult]) => {
      if (cancelled) return;
      if (summaryResult.status === 'fulfilled') setDashboardSummary(summaryResult.value);
      if (paramsResult.status === 'fulfilled') {
        const list = Array.isArray(paramsResult.value) ? paramsResult.value : (paramsResult.value?.content || []);
        setDashboardParams(list);
        setTrendParameter((current) => current || String(list.find((item) => /ph/i.test(item.nombreParametro || item.nombre || ''))?.idParametro || list[0]?.idParametro || ''));
      }
      if (profileResult.status === 'fulfilled') {
        setProfilePhoto(profileResult.value.fotoPerfil || '');
        setDisplayName(`${profileResult.value.nombre || ''} ${profileResult.value.apellido || ''}`.trim() || profileResult.value.correo || '');
      }
    });
    return () => { cancelled = true; };
  }, [authenticated]);

  useEffect(() => {
    if (!authenticated || !trendParameter) return;
    let cancelled = false;
    apiRequest(`/api/dashboard/tendencia?idParametro=${encodeURIComponent(trendParameter)}`)
      .then((data) => { if (!cancelled) { setTrendError(''); setTrendData(data); } })
      .catch((error) => { if (!cancelled) { setTrendData(null); setTrendError(error.message || 'No se pudo cargar la tendencia.'); } });
    return () => { cancelled = true; };
  }, [authenticated, trendParameter]);

  const handleForgotPassword = async (event) => {
    event?.preventDefault();
    const correo = loginEmail.trim().toLowerCase();
    if (!correo) { setAuthError('Ingresa el correo de tu cuenta.'); return; }
    setAuthBusy(true); setAuthError(''); setAuthNotice('');
    try {
      await apiRequest('/api/auth/password/solicitar-recuperacion', { method: 'POST', body: JSON.stringify({ correo }) });
      setRecoveryCodeSent(true);
      setResetForm({ codigo: '', nueva: '', confirmar: '' });
      setAuthNotice('Si el correo corresponde a una cuenta activa, recibirás un código de verificación. Revisa también la carpeta de correo no deseado.');
    } catch (error) { setAuthError(error.message || 'No se pudo enviar el código de recuperación.'); }
    finally { setAuthBusy(false); }
  };

  const handleConfirmPasswordReset = async (event) => {
    event.preventDefault(); setAuthError(''); setAuthNotice('');
    if (!/^\d{8}$/.test(resetForm.codigo)) { setAuthError('Ingresa el código de 8 dígitos que recibiste por correo.'); return; }
    if (resetForm.nueva !== resetForm.confirmar) { setAuthError('Las contraseñas no coinciden.'); return; }
    if (resetForm.nueva.length < 8 || resetForm.nueva.length > 72) { setAuthError('La contraseña debe tener entre 8 y 72 caracteres.'); return; }
    setAuthBusy(true);
    try {
      await apiRequest('/api/auth/password/confirmar-recuperacion', { method: 'POST', body: JSON.stringify({ correo: loginEmail.trim().toLowerCase(), codigo: resetForm.codigo, passwordNueva: resetForm.nueva, confirmarPassword: resetForm.confirmar }) });
      setLoginPassword(''); setResetForm({ codigo: '', nueva: '', confirmar: '' }); setRecoveryCodeSent(false);
      setEntryScreen('login'); setAuthNotice('Contraseña actualizada. Ya puedes iniciar sesión con tu nueva contraseña.');
    } catch (error) { setAuthError(error.message || 'El código no es válido o venció. Solicita uno nuevo.'); }
    finally { setAuthBusy(false); }
  };

  const handleLogin = async (event) => {
    event.preventDefault();
    setAuthBusy(true); setAuthError(''); setAuthNotice('');
    try {
      const user = await apiLogin(loginEmail.trim(), loginPassword);
      setSessionUser(user);
      setDisplayName(user.nombreCompleto || user.correo || '');
      setRole(roleForNavigation(user.rol));
      setView('dashboard'); setAuthenticated(true); setEntryScreen('welcome');
      setLoginPassword('');
      setAlerts([]); setRecords([]); setRecordsLoadError(''); setRemotePoints([]); setPublishedUnreadCount(0); setPersonalNotificationCount(0); setPublishedAlerts([]);
      const [alertsResult, pointsResult, recordsResult] = await Promise.allSettled([
        fetchAllAlertas(),
        apiRequest(user.rol === 'ADMINISTRADOR' ? '/api/puntos-monitoreo' : '/api/puntos-monitoreo?estado=ACTIVO'),
        fetchAllMonitoreos(),
      ]);
      const loadErrors = [];
      if (alertsResult.status === 'fulfilled') setAlerts(responseRows(alertsResult.value).map(toUiAlert));
      else loadErrors.push(`alertas: ${alertsResult.reason?.message || 'no disponibles'}`);
      if (pointsResult.status === 'fulfilled') setRemotePoints(responseRows(pointsResult.value));
      else loadErrors.push(`estaciones: ${pointsResult.reason?.message || 'no disponibles'}`);
      if (recordsResult.status === 'fulfilled') {
        setRecords(recordsResult.value.map(toUiRecord));
        setRecordsLoadError('');
      } else {
        const message = recordsResult.reason?.message || 'No se pudo consultar el historial de lecturas.';
        setRecordsLoadError(message);
        loadErrors.push(`historial de lecturas: ${message}`);
      }
      try {
        const notices = await apiRequest('/api/alertas-publicadas');
        if (Array.isArray(notices)) { setPublishedAlerts(notices); setPublishedUnreadCount(notices.filter((item) => !item.leida).length); }
      } catch (noticeError) { loadErrors.push(`avisos: ${noticeError.message || 'no disponibles'}`); }
      if (loadErrors.length) notify(`Sesión iniciada. Algunos datos no se pudieron cargar: ${loadErrors.join('; ')}`);
    } catch (error) {
      const message = error.message || '';
      setAuthError(message.toLowerCase().includes('credenciales') || message.toLowerCase().includes('contraseña incorrectos')
        ? 'Correo electrónico o contraseña incorrectos.'
        : (message || 'No se pudo iniciar sesión. Comprueba tu conexión e inténtalo nuevamente.'));
    }
    finally { setAuthBusy(false); }
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setAuthError(''); setAuthNotice('');
    const requiredFields = ['nombre', 'apellido', 'correo', 'usuarioLogin', 'telefono', 'password', 'confirmarPassword'];
    setRegisterTouched(Object.fromEntries(requiredFields.map((field) => [field, true])));
    const invalid = requiredFields.some((field) => registerFieldMessage(field, registerForm));
    if (invalid) {
      setAuthError('Revisa los mensajes en rojo debajo de los campos e inténtalo nuevamente.');
      return;
    }
    setAuthBusy(true);
    try {
      await registerAccount({
        nombre: registerForm.nombre.trim(),
        apellido: registerForm.apellido.trim(),
        correo: registerForm.correo.trim().toLowerCase(),
        usuarioLogin: registerForm.usuarioLogin.trim(),
        password: registerForm.password,
        telefono: registerForm.telefono.trim() || null,
      });
      setLoginEmail(registerForm.correo.trim().toLowerCase());
      setLoginPassword('');
      setRegisterForm({ nombre: '', apellido: '', correo: '', usuarioLogin: '', password: '', confirmarPassword: '', telefono: '' });
      setRegisterTouched({});
      setEntryScreen('login');
      setAuthNotice('Tu cuenta se creó correctamente. Ahora puedes iniciar sesión con tu correo y contraseña.');
    } catch (error) {
      setAuthError(error.message || 'No se pudo crear la cuenta. Comprueba los datos e inténtalo nuevamente.');
    } finally {
      setAuthBusy(false);
    }
  };

  const handleLogout = () => { clearSession(); setAuthenticated(false); setSessionUser(null); setProfilePhoto(''); setDashboardSummary(null); setTrendData(null); setDashboardParams([]); setTrendParameter(''); setPublishedAlerts([]); setPublishedUnreadCount(0); setPersonalNotificationCount(0); setRecords([]); setRecordsLoadError(''); setAlerts([]); setRemotePoints([]); setView('dashboard'); setMobileNav(false); setEntryScreen('welcome'); };
  const isAdmin = role === 'Administrador';
  const handlePublishedUnreadChange = useCallback((count) => setPublishedUnreadCount(Number(count) || 0), []);
  const handlePublishedAlertsChange = useCallback((items) => setPublishedAlerts(Array.isArray(items) ? items : []), []);
  const handlePersonalNotificationCountChange = useCallback((count) => setPersonalNotificationCount(Number(count) || 0), []);
  useEffect(() => {
    if (!authenticated) return undefined;
    let cancelled = false;
    apiRequest('/api/alertas-publicadas')
      .then((items) => { if (!cancelled) { const list = Array.isArray(items) ? items : []; setPublishedAlerts(list); setPublishedUnreadCount(list.filter((item) => !item.leida).length); } })
      .catch(() => { if (!cancelled) { setPublishedAlerts([]); setPublishedUnreadCount(0); } });
    return () => { cancelled = true; };
  }, [authenticated, sessionUser?.idUsuario]);
  useEffect(() => {
    if (!authenticated) return undefined;
    let cancelled = false;
    const refreshCount = () => apiRequest('/api/notificaciones/no-leidas/total')
      .then((result) => { if (!cancelled) setPersonalNotificationCount(Number(result?.total) || 0); })
      .catch(() => { if (!cancelled) setPersonalNotificationCount(0); });
    refreshCount();
    const interval = window.setInterval(refreshCount, 60000);
    return () => { cancelled = true; window.clearInterval(interval); };
  }, [authenticated, sessionUser?.idUsuario]);
  const handleProfileUpdated = useCallback((profile) => { setProfilePhoto(profile.fotoPerfil || ''); setDisplayName(`${profile.nombre || ''} ${profile.apellido || ''}`.trim() || profile.correo || ''); setSessionUser((old) => old ? { ...old, nombreCompleto: `${profile.nombre || ''} ${profile.apellido || ''}`.trim(), fotoPerfil: profile.fotoPerfil || '' } : old); }, []);
  const allowedIds = sessionUser?.rol === 'AUDITOR' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'notificaciones', 'reportes', 'configuracion'] : sessionUser?.rol === 'GERENTE' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'notificaciones', 'reportes', 'configuracion'] : sessionUser?.rol === 'SUPERVISOR' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'notificaciones', 'reportes', 'configuracion'] : null;
  const visibleNavigation = navigation
    .filter((item) => !(isAdmin && item.id === 'usuarios'))
    .filter((item) => item.id !== 'configuracion' || isAdmin)
    .filter((item) => item.roles.includes(role) && (!allowedIds || allowedIds.includes(item.id)))
    .map((item) => item.id === 'configuracion' && isAdmin ? { ...item, label: 'Configuración', icon: 'settings' } : item);
  const activeNavigation = visibleNavigation.find((item) => item.id === view) || visibleNavigation[0];

  const markReadingModalHandled = useCallback(() => setOpenAddReading(false), []);

  const notify = (message) => {
    setToast(message);
    if (toastTimer.current) window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(''), 4200);
  };

  const refreshStationsAndReadings = useCallback(async () => {
    const stationPath = isAdmin ? '/api/puntos-monitoreo' : '/api/puntos-monitoreo?estado=ACTIVO';
    const [pointsResult, recordsResult, summaryResult, alertsResult, publishedResult] = await Promise.allSettled([
      apiRequest(stationPath),
      fetchAllMonitoreos(),
      apiRequest('/api/dashboard/resumen'),
      fetchAllAlertas(),
      apiRequest('/api/alertas-publicadas'),
    ]);

    const errors = [];
    if (pointsResult.status === 'fulfilled') setRemotePoints(responseRows(pointsResult.value));
    else errors.push(`estaciones: ${pointsResult.reason?.message || 'no disponibles'}`);

    if (recordsResult.status === 'fulfilled') {
      setRecords(recordsResult.value.map(toUiRecord));
      setRecordsLoadError('');
    } else {
      const message = recordsResult.reason?.message || 'no disponible';
      setRecordsLoadError(message);
      errors.push(`historial de lecturas: ${message}`);
    }

    if (summaryResult.status === 'fulfilled') setDashboardSummary(summaryResult.value);
    if (alertsResult.status === 'fulfilled') setAlerts(responseRows(alertsResult.value).map(toUiAlert));
    if (publishedResult.status === 'fulfilled' && Array.isArray(publishedResult.value)) {
      setPublishedAlerts(publishedResult.value);
      setPublishedUnreadCount(publishedResult.value.filter((item) => !item.leida).length);
    }

    if (errors.length) throw new Error(`No se pudo actualizar ${errors.join(' y ')}.`);
  }, [isAdmin]);

  useEffect(() => {
    if (!authenticated || !['dashboard', 'estaciones', 'reportes'].includes(view)) return undefined;
    let cancelled = false;
    fetchAllMonitoreos()
      .then((rows) => {
        if (!cancelled) {
          setRecords(rows.map(toUiRecord));
          setRecordsLoadError('');
        }
      })
      .catch((error) => {
        if (!cancelled) setRecordsLoadError(error.message || 'No se pudo consultar el historial de lecturas.');
      });
    return () => { cancelled = true; };
  }, [authenticated, view]);

  const navigate = (next) => {
    const allowed = visibleNavigation.some((item) => item.id === next);
    if (!allowed) { notify('Tu perfil no tiene permiso para abrir esa sección.'); return; }
    setView(next);
    setMobileNav(false);
    setToast('');
    if (next === 'estaciones' || next === 'alertas') {
      refreshStationsAndReadings().catch((error) => notify(error.message || 'No se pudieron actualizar las mediciones y alertas.'));
    }
  };



  const addAudit = (event) => setAudit((current) => [{
    id: `AUD-${String(current.length + 1).padStart(4, '0')}`,
    date: `${todayDisplay()} ${new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`,
    origin: role,
    ...event,
  }, ...current]);

  const openEditor = (kind, item) => {
    if (!isAdmin) { notify('Solo el administrador puede corregir información.'); return; }
    setEditor({ kind, id: item.id });
    setEditorForm(kind === 'report'
      ? { title: item.title || '', category: item.category || '', area: item.area || '', description: item.description || '', status: item.status || 'Pendiente de revisión' }
      : { ph: String(item.ph ?? ''), turbidity: String(item.turbidity ?? ''), temperature: String(item.temperature ?? ''), flow: String(item.flow ?? ''), observation: item.observation || '' });
  };

  const saveEditor = async (event) => {
    event.preventDefault();
    if (!isAdmin || !editor || editorBusy) return;
    if (editor.kind === 'report') {
      if (!editorForm.title.trim() || !editorForm.description.trim()) { notify('El título y la descripción son obligatorios.'); return; }
      const original = reports.find((item) => item.id === editor.id);
      setReports((current) => current.map((item) => item.id === editor.id ? { ...item, ...editorForm, title: editorForm.title.trim(), description: editorForm.description.trim() } : item));
      addAudit({ event: 'Reporte corregido', station: original?.area || 'No indicada', detail: `${editor.id}: texto o estado actualizado`, status: 'Registrado', kind: 'warning' });
      notify('Reporte actualizado en la vista actual. Los reportes ciudadanos reales se gestionan en Centro de alertas.');
      setEditor(null);
      return;
    }
    const numbers = { ph:Number(editorForm.ph), turbidity:Number(editorForm.turbidity), temperature:Number(editorForm.temperature), flow:Number(editorForm.flow) };
    if (Object.values(numbers).some((value) => !Number.isFinite(value) || value < 0) || numbers.ph > 14) { notify('Revisa los valores: deben ser números válidos y el pH debe estar entre 0 y 14.'); return; }
    setEditorBusy(true);
    try {
      // Leemos la medición real para conservar el punto, tipo de muestreo y parámetros adicionales.
      const current = await apiRequest(`/api/monitoreos/${editor.id}`);
      const lecturas = Array.isArray(current.lecturas) ? current.lecturas : [];
      const found = { ph:false, turbidity:false, temperature:false, flow:false };
      const normalized = lecturas.map((reading) => {
        const code = String(reading.codigo || '').toUpperCase();
        const name = String(reading.parametro || reading.nombreParametro || '').toLowerCase();
        let key = null;
        if (code === 'PH' || name === 'ph' || name.includes('hidrógeno')) key = 'ph';
        else if (code.includes('TURB') || name.includes('turbidez')) key = 'turbidity';
        else if (code.includes('TEMP') || name.includes('temperatura')) key = 'temperature';
        else if (code.includes('CAUD') || name.includes('caudal') || name.includes('flujo')) key = 'flow';
        if (key) found[key] = true;
        return { idParametro:reading.idParametro, valor:key ? numbers[key] : Number(reading.valor) };
      });
      if (Object.values(found).some((value) => !value)) throw new Error('No se pudieron identificar pH, turbidez, temperatura y caudal en este registro. No se guardaron cambios.');
      const saved = await apiRequest(`/api/monitoreos/${editor.id}`, { method:'PUT', body:JSON.stringify({ idPunto:current.idPunto, tipoMuestreo:current.tipoMuestreo, horaMuestreo:current.horaMuestreo, observacion:editorForm.observation.trim() || null, lecturas:normalized }) });
      const updated = toUiRecord(saved);
      setRecords((items) => items.map((item) => item.id === editor.id ? updated : item));
      addAudit({ event:'Registro corregido', station:updated.station, detail:`${editor.id}: registro corregido por el administrador; el cambio quedó registrado en el historial de actividad`, status:'Registrado', kind:'warning' });
      try {
        const alertResult = await fetchAllAlertas();
        setAlerts(alertResult.map(toUiAlert));
      } catch { /* La corrección ya se guardó; la actualización visual de alertas puede reintentarse. */ }
      notify('Corrección guardada correctamente. Las alertas fueron actualizadas.');
      setEditor(null);
    } catch (error) {
      notify(`No se guardó la corrección: ${error.message || 'problema al guardar los cambios'}`);
    } finally { setEditorBusy(false); }
  };

  const submitMeasurement = async (event) => {
    event.preventDefault();
    if (!isAdmin) return;
    const formElement = event.currentTarget;
    const formData = new FormData(formElement);
    const value = Object.fromEntries(formData.entries());
    const numbers = { ph: Number(value.ph), turbidity: Number(value.turbidity), temperature: Number(value.temperature), flow: Number(value.flow) };
    if (Object.values(numbers).some((number) => !Number.isFinite(number) || number < 0) || numbers.ph > 14) { notify('Verifica los parámetros ingresados.'); return; }
    try {
      const points = remotePoints.length ? remotePoints : await apiRequest('/api/puntos-monitoreo?estado=ACTIVO');
      const activePoints = points.filter((point) => point.estado === 'ACTIVO');
      const pointId = Number(String(value.station).split(' · ')[0]);
      const selectedPoint = activePoints.find((point) => Number(point.idPunto) === pointId);
      if (!selectedPoint) throw new Error('No se encontró la estación seleccionada. Actualiza la pantalla e inténtalo nuevamente.');
      const parameters = await apiRequest('/api/parametros?estado=ACTIVO');
      const parameterList = Array.isArray(parameters) ? parameters : (parameters.content || []);
      const matchParam = (patterns) => parameterList.find((p) => patterns.some((pattern) => String(p.nombreParametro || p.nombre || '').toLowerCase().includes(pattern)));
      const pH = matchParam(['ph', 'potencial de hidrógeno']);
      const turb = matchParam(['turbidez']);
      const temp = matchParam(['temperatura']);
      const flow = matchParam(['caudal', 'flujo']);
      if ([pH, turb, temp, flow].some((p) => !p?.idParametro)) throw new Error('Faltan los cuatro parámetros necesarios (pH, turbidez, temperatura y caudal). No se guardó la medición.');
      const saved = await apiRequest('/api/monitoreos', { method: 'POST', body: JSON.stringify({ idPunto: pointId, tipoMuestreo: 'MANUAL', horaMuestreo: `${value.date}T${value.time}:00`, observacion: value.observation || '', lecturas: [{ idParametro: pH.idParametro, valor: numbers.ph }, { idParametro: turb.idParametro, valor: numbers.turbidity }, { idParametro: temp.idParametro, valor: numbers.temperature }, { idParametro: flow.idParametro, valor: numbers.flow }] }) });
      const record = { id: String(saved.idMonitoreo ?? saved.id ?? 'Guardado'), station: selectedPoint.nombrePunto, date: value.date.split('-').reverse().join('/'), time: value.time, ...numbers, observation: value.observation || 'Sin observaciones' };
      setRecords((current) => [record, ...current]);
      if (saved.alertas?.length) setAlerts((current) => [...saved.alertas.map((a) => ({ id: String(a.idAlerta ?? a.id ?? ''), level: String(a.nivel || 'MEDIA').includes('ALTA') ? 'Alta' : String(a.nivel || 'MEDIA').includes('BAJA') ? 'Baja' : 'Media', title: a.descripcion || a.mensaje || 'Alerta generada por el sistema', station: selectedPoint.nombrePunto, value: 'Ver detalle en el sistema', limit: 'Según umbral configurado', time: `${record.date} · ${record.time}`, status: a.estado || 'ABIERTA' })), ...current]);
      try {
        await refreshStationsAndReadings();
        const latestSummary = await apiRequest('/api/dashboard/resumen');
        setDashboardSummary(latestSummary);
      } catch { /* El registro ya fue confirmado por POST; la consulta se puede repetir con Actualizar. */ }
      notify('Medición guardada correctamente. El resumen y el historial fueron actualizados.');
      formElement.reset();
    } catch (error) {
      notify(`No se pudo guardar la medición: ${error.message}. Verifica los datos ingresados y tus permisos.`);
      return;
    }
  };


  const matchingIndicator = (patterns) => (dashboardSummary?.indicadores || []).find((indicator) => patterns.some((pattern) => `${indicator.parametro || ''} ${indicator.codigo || ''}`.toLowerCase().includes(pattern)));
  const formatIndicator = (patterns) => { const indicator = matchingIndicator(patterns); return indicator?.ultimoValor == null ? '—' : Number(indicator.ultimoValor).toLocaleString('es-PE', { maximumFractionDigits: 3 }); };
  const indicatorUnit = (patterns, fallback) => matchingIndicator(patterns)?.unidadMedida || fallback;
  const indicatorDetail = (patterns) => { const i = matchingIndicator(patterns); if (!i) return dashboardSummary ? 'Sin lecturas registradas' : 'Esperando información de monitoreo'; const when = i.fecha ? new Date(i.fecha).toLocaleString('es-PE') : ''; return `${i.punto || 'Punto no indicado'}${when ? ` · ${when}` : ''}`; };
  const activeStations = useMemo(() => remotePoints.map((point) => { const record = records.find((r) => r.station === point.nombrePunto); return { id: String(point.idPunto), name: point.nombrePunto, area: point.area || '—', ph: record?.ph ?? '—', turbidity: record?.turbidity ?? '—', flow: record?.flow ?? '—', temperature: record?.temperature ?? '—', status: point.estado || '—' }; }), [remotePoints, records]);
  // El indicador rojo representa exclusivamente comunicados publicados por el administrador.
  const unreadPublishedAlertCount = publishedUnreadCount;

  if (!authenticated) return <main className="welcome-screen" onSubmitCapture={validateFormFields}>
    <header className="welcome-header"><div className="brand"><span className="brand-mark"><Icon name="droplet" size={25}/></span><span><strong>CERRO VERDE</strong><small>SISTEMA DE MONITOREO HÍDRICO</small></span></div><span className="welcome-secure"><Icon name="shield" size={15}/> Acceso seguro</span></header>
    <div className="welcome-content"><section className="welcome-copy"><span className="welcome-kicker">PLATAFORMA DE INFORMACIÓN AMBIENTAL</span><h1>Datos claros para cuidar <em>cada recurso.</em></h1><p>Consulta el sistema de monitoreo, revisa información ambiental y gestiona las tareas autorizadas según tu cuenta.</p><div className="welcome-art" aria-hidden="true"><div className="art-glow"/><div className="server-rack"><span/><span/><span/><span/></div><div className="art-wave wave-one"/><div className="art-wave wave-two"/><div className="art-dot dot-one"/><div className="art-dot dot-two"/></div><div className="welcome-features"><span><Icon name="activity" size={16}/> Monitoreo</span><span><Icon name="map" size={16}/> Puntos y mapas</span><span><Icon name="bell" size={16}/> Alertas</span></div></section>
      <section className="welcome-panel">
  {entryScreen === 'welcome' ? <>
    <span className="eyebrow">BIENVENIDO</span>
    <h2>¿Cómo deseas continuar?</h2>
    <p>Ingresa con una cuenta existente para acceder a las funciones disponibles para tu rol.</p>
    <button className="welcome-action primary" onClick={() => { setEntryScreen('login'); setAuthError(''); setAuthNotice(''); }}>
      <span className="welcome-action-icon"><Icon name="log-in" size={21}/></span>
      <span><strong>Iniciar sesión</strong><small>Accede con tu correo y contraseña</small></span>
      <Icon name="arrow-right" size={18}/>
    </button>
    <button className="welcome-action" onClick={() => { setEntryScreen('register'); setAuthError(''); setAuthNotice(''); }}>
      <span className="welcome-action-icon register-icon"><Icon name="user-plus" size={21}/></span>
      <span><strong>Registrarse</strong><small>Crea tu propia cuenta</small></span>
      <Icon name="arrow-right" size={18}/>
    </button>
    <div className="welcome-footnote"><Icon name="lock" size={16}/><span>La identidad y los permisos se verifican en el servidor.</span></div>
  </> : entryScreen === 'login' ? <>
    <button className="back-link" onClick={() => { setEntryScreen('welcome'); setAuthError(''); setAuthNotice(''); }}><Icon name="arrow-left" size={15}/> Volver</button>
    <span className="eyebrow">ACCESO A LA PLATAFORMA</span>
    <h2>Iniciar sesión</h2>
    <p>Utiliza las credenciales de una cuenta registrada en el sistema.</p>
    {authNotice && <div className="auth-success" role="status"><Icon name="check" size={16}/>{authNotice}</div>}
    <form className="auth-form" onSubmit={handleLogin}>
      <div className="field"><label htmlFor="login-email">Correo electrónico</label><input id="login-email" type="email" autoComplete="username" value={loginEmail} onChange={(e) => setLoginEmail(e.target.value)} placeholder="nombre@dominio.com" required/></div>
      <div className="field"><label htmlFor="login-password">Contraseña</label><input id="login-password" type="password" autoComplete="current-password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} placeholder="Ingresa tu contraseña" required/></div>
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? 'Ingresando…' : 'Entrar al sistema'} <Icon name="arrow-right" size={17}/></button>
    </form>
    <button type="button" className="text-link forgot-link" onClick={() => { setEntryScreen('forgot'); setRecoveryCodeSent(false); setResetForm({ codigo: '', nueva: '', confirmar: '' }); setAuthError(''); setAuthNotice(''); }}>Olvidé mi contraseña</button>
    <div className="login-help"><Icon name="info" size={16}/><span>Inicia sesión con tu cuenta. Si tienes problemas para acceder, contacta al administrador del sistema.</span></div>
  </> : entryScreen === 'forgot' ? <>
    <button className="back-link" onClick={() => { setEntryScreen('login'); setAuthError(''); setAuthNotice(''); setRecoveryCodeSent(false); setResetForm({ codigo: '', nueva: '', confirmar: '' }); }}><Icon name="arrow-left" size={15}/> Volver al inicio de sesión</button>
    <span className="eyebrow">RECUPERACIÓN DE CUENTA</span><h2>Olvidé mi contraseña</h2><p>Escribe el correo de tu cuenta. Verificaremos que tengas acceso al correo antes de actualizar la contraseña.</p>
    <form className="auth-form" data-match-fields="reset-new,reset-confirm" onSubmit={recoveryCodeSent ? handleConfirmPasswordReset : handleForgotPassword}>
      <div className="field"><label htmlFor="forgot-email">Correo electrónico</label><input id="forgot-email" type="email" autoComplete="email" value={loginEmail} onChange={(e)=>{setLoginEmail(e.target.value);setRecoveryCodeSent(false);setResetForm({ codigo: '', nueva: '', confirmar: '' });setAuthError('');setAuthNotice('');}} placeholder="nombre@dominio.com" required/></div>
      {recoveryCodeSent && <>
        <div className="field"><label htmlFor="recovery-code">Código de verificación (8 dígitos)</label><input id="recovery-code" type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{8}" maxLength="8" value={resetForm.codigo} onChange={(e)=>setResetForm({...resetForm,codigo:e.target.value.replace(/\D/g,'').slice(0,8)})} placeholder="00000000" required/></div>
        <div className="field"><label htmlFor="reset-new">Nueva contraseña</label><input id="reset-new" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={resetForm.nueva} onChange={(e)=>setResetForm({...resetForm,nueva:e.target.value})} placeholder="Mínimo 8 caracteres" required/></div>
        <div className="field"><label htmlFor="reset-confirm">Confirmar nueva contraseña</label><input id="reset-confirm" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={resetForm.confirmar} onChange={(e)=>setResetForm({...resetForm,confirmar:e.target.value})} placeholder="Repite la nueva contraseña" required/></div>
      </>}
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}{authNotice && <div className="auth-success" role="status"><Icon name="check" size={16}/>{authNotice}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? (recoveryCodeSent ? 'Actualizando…' : 'Enviando código…') : (recoveryCodeSent ? 'Cambiar contraseña' : 'Enviar código de verificación')} <Icon name={recoveryCodeSent ? 'check' : 'send'} size={16}/></button>
      {recoveryCodeSent && <button type="button" className="text-link forgot-link" onClick={handleForgotPassword} disabled={authBusy}>Enviar un código nuevo</button>}
    </form>
    <div className="login-help"><Icon name="lock" size={16}/><span>El código vence en 10 minutos y permite un máximo de 5 intentos. No compartas el código con otras personas.</span></div>
  </> : <>
    <button className="back-link" onClick={() => { setEntryScreen('welcome'); setAuthError(''); setAuthNotice(''); }}><Icon name="arrow-left" size={15}/> Volver</button>
    <span className="eyebrow">CREACIÓN DE CUENTAS</span>
    <h2>Crear cuenta</h2>
    <p>Completa tus datos para registrar una cuenta en el sistema. Los campos con * son obligatorios.</p>
    <form className="auth-form register-form" data-match-fields="register-password,register-confirm-password" onSubmit={handleRegister}>
      <div className="field"><label htmlFor="register-nombre">Nombres *</label><input id="register-nombre" autoComplete="given-name" maxLength="80" value={registerForm.nombre} onChange={(e) => updateRegisterField('nombre', e.target.value)} onBlur={() => touchRegisterField('nombre')} aria-invalid={Boolean(registerFieldError('nombre'))} placeholder="Tus nombres" required/>{registerFieldError('nombre') && <small className="field-error">{registerFieldError('nombre')}</small>}</div>
      <div className="field"><label htmlFor="register-apellido">Apellidos *</label><input id="register-apellido" autoComplete="family-name" maxLength="80" value={registerForm.apellido} onChange={(e) => updateRegisterField('apellido', e.target.value)} onBlur={() => touchRegisterField('apellido')} aria-invalid={Boolean(registerFieldError('apellido'))} placeholder="Tus apellidos" required/>{registerFieldError('apellido') && <small className="field-error">{registerFieldError('apellido')}</small>}</div>
      <div className="field"><label htmlFor="register-correo">Correo electrónico *</label><input id="register-correo" type="email" autoComplete="email" maxLength="120" value={registerForm.correo} onChange={(e) => updateRegisterField('correo', e.target.value)} onBlur={() => touchRegisterField('correo')} aria-invalid={Boolean(registerFieldError('correo'))} placeholder="nombre@dominio.com" required/>{registerFieldError('correo') && <small className="field-error">{registerFieldError('correo')}</small>}</div>
      <div className="field"><label htmlFor="register-usuario">Nombre de usuario *</label><input id="register-usuario" autoComplete="username" minLength="3" maxLength="40" value={registerForm.usuarioLogin} onChange={(e) => updateRegisterField('usuarioLogin', e.target.value)} onBlur={() => touchRegisterField('usuarioLogin')} aria-invalid={Boolean(registerFieldError('usuarioLogin'))} placeholder="Ej. maria.lopez" required/>{registerFieldError('usuarioLogin') && <small className="field-error">{registerFieldError('usuarioLogin')}</small>}</div>
      <div className="field"><label htmlFor="register-telefono">Celular (opcional)</label><input id="register-telefono" type="tel" inputMode="numeric" autoComplete="tel" maxLength="9" value={registerForm.telefono} onChange={(e) => updateRegisterField('telefono', e.target.value.replace(/\D/g, '').slice(0, 9))} onBlur={() => touchRegisterField('telefono')} aria-invalid={Boolean(registerFieldError('telefono'))} placeholder="9XXXXXXXX"/>{registerFieldError('telefono') && <small className="field-error">{registerFieldError('telefono')}</small>}</div>
      <div className="field"><label htmlFor="register-password">Contraseña *</label><input id="register-password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={registerForm.password} onChange={(e) => updateRegisterField('password', e.target.value)} onBlur={() => touchRegisterField('password')} aria-invalid={Boolean(registerFieldError('password'))} placeholder="Mínimo 8 caracteres" required/>{registerFieldError('password') && <small className="field-error">{registerFieldError('password')}</small>}</div>
      <div className="field"><label htmlFor="register-confirm-password">Confirmar contraseña *</label><input id="register-confirm-password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={registerForm.confirmarPassword} onChange={(e) => updateRegisterField('confirmarPassword', e.target.value)} onBlur={() => touchRegisterField('confirmarPassword')} aria-invalid={Boolean(registerFieldError('confirmarPassword'))} placeholder="Repite la contraseña" required/>{registerFieldError('confirmarPassword') && <small className="field-error">{registerFieldError('confirmarPassword')}</small>}</div>
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? 'Creando cuenta…' : 'Crear cuenta'} <Icon name="user-plus" size={17}/></button>
    </form>
    <div className="login-help"><Icon name="shield-check" size={16}/><span>La contraseña se protege en el servidor. Las cuentas nuevas reciben permisos de auditoría y no pueden administrar usuarios ni cambiar roles.</span></div>
  </>}
</section></div><footer className="welcome-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>Conexión segura</span></footer>
  </main>;

  return <div className={`app-shell ${mobileNav ? 'nav-open' : ''}`} onSubmitCapture={validateFormFields}>
    {mobileNav && <button className="mobile-scrim" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"/>}
    <aside className="sidebar"><div className="brand sidebar-brand"><span className="brand-mark"><Icon name="droplet" size={23}/></span><span><strong>CERRO VERDE</strong><small>MONITOREO HÍDRICO</small></span><button className="mobile-close" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"><Icon name="close"/></button></div>
      <div className="sidebar-project"><span className="sidebar-project-dot"/><div><strong>Área de trabajo</strong><small>Panel ambiental</small></div></div><div className="nav-label">MENÚ PRINCIPAL</div>
      <nav className="sidebar-nav" aria-label="Navegación principal">{visibleNavigation.map((item) => <button key={item.id} className={`nav-link ${view === item.id ? 'nav-link-active' : ''}`} onClick={() => navigate(item.id)}><Icon name={item.icon} size={19}/><span>{item.label}</span>{item.id === 'alertas' && unreadPublishedAlertCount > 0 && <small className="nav-count">{unreadPublishedAlertCount}</small>}{item.id === 'notificaciones' && personalNotificationCount > 0 && <small className="nav-count">{personalNotificationCount}</small>}</button>)}</nav>
      <div className="sidebar-account"><div className="avatar avatar-photo">{profilePhoto ? <img src={profilePhoto} alt="Foto de perfil"/> : (isAdmin ? 'A' : 'U')}</div><div className="account-meta"><strong>Mi perfil</strong><small>{displayName || (isAdmin ? 'Administrador' : 'Usuario normal')}</small></div></div>
      <button type="button" className="logout-button" onClick={handleLogout} aria-label="Cerrar sesión"><Icon name="logout" size={18}/><span>Cerrar sesión</span></button>
    </aside>
    <main className="main-column"><header className="topbar"><div className="topbar-heading"><button className="mobile-menu" onClick={() => setMobileNav(true)} aria-label="Abrir menú"><Icon name="menu" size={22}/></button><div><span>Panel de control</span><strong>{activeNavigation?.label || 'Resumen general'}</strong></div></div><div className="topbar-right"><span className="demo-status"><i/>Conectado como {roleLabel(sessionUser?.rol)}</span><button className="topbar-alert" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell" size={19}/>{unreadPublishedAlertCount > 0 && <i/>}</button><span className="topbar-separator"/><div className="topbar-profile"><span className="avatar avatar-small avatar-photo">{profilePhoto ? <img src={profilePhoto} alt="Foto de perfil"/> : (isAdmin ? 'A' : 'U')}</span><div><strong>{displayName || (isAdmin ? 'Administrador' : 'Usuario normal')}</strong><small>{isAdmin ? 'Administrador' : 'Usuario normal'}</small></div></div></div></header>
      <div className="content-area">
        {view === 'dashboard' && <div className="page-stack"><SectionTitle kicker="VISTA GENERAL / 01" title="Resumen del monitoreo" description="Consulta el estado general del monitoreo y las últimas mediciones registradas." action={isAdmin ? <button className="button button-primary" onClick={() => navigate('monitoreo')}><Icon name="plus" size={16}/> Registrar medición</button> : <button className="button button-primary" onClick={() => navigate('alertas')}><Icon name="send" size={16}/> Enviar observación</button>}/><div className="metrics-grid"><MetricCard label="Puntos de monitoreo activos" value={String(dashboardSummary?.puntosActivos ?? '—')} unit="puntos" detail={dashboardSummary ? 'Puntos registrados' : 'Esperando información de monitoreo'} icon="pin" tone="teal"/><MetricCard label="Última lectura de pH" value={formatIndicator(['ph', 'hidrógeno'])} unit={indicatorUnit(['ph', 'hidrógeno'], 'pH')} detail={indicatorDetail(['ph', 'hidrógeno'])} icon="droplet" tone="blue"/><MetricCard label="Última lectura de turbidez" value={formatIndicator(['turbidez'])} unit={indicatorUnit(['turbidez'], 'NTU')} detail={indicatorDetail(['turbidez'])} icon="activity" tone="green"/><MetricCard label="Última lectura de caudal" value={formatIndicator(['caudal', 'flujo'])} unit={indicatorUnit(['caudal', 'flujo'], 'm³/s')} detail={indicatorDetail(['caudal', 'flujo'])} icon="activity" tone="amber"/><MetricCard label="Eventos de medición abiertos" value={String(dashboardSummary?.alertas?.noCerradas ?? '—')} unit="eventos" detail={dashboardSummary ? `${dashboardSummary.alertas?.alta || 0} de prioridad alta` : 'Esperando información de monitoreo'} icon="activity" tone="amber"/></div>
        <section className="panel dashboard-trend-panel"><div className="panel-heading"><div><h2>Tendencia de monitoreo</h2><p>Promedios diarios y unidad de medida de las mediciones registradas.</p></div><label className="trend-parameter-picker"><span>Parámetro</span><select value={trendParameter} onChange={(e)=>{setTrendError('');setTrendParameter(e.target.value);}} disabled={!dashboardParams.length}>{dashboardParams.map((param)=> <option key={param.idParametro} value={param.idParametro}>{param.nombreParametro || param.nombre}</option>)}</select></label></div>{trendError && <div className="auth-error" role="alert">{trendError}</div>}{trendData && <div className="trend-summary"><div><small>Parámetro</small><strong>{trendData.parametro}</strong></div><div><small>Desde</small><strong>{trendData.desde}</strong></div><div><small>Hasta</small><strong>{trendData.hasta}</strong></div></div>}<TrendChart data={trendData} unit={trendData?.unidadMedida}/></section>
        <div className="dashboard-live-info"><Icon name="database" size={18}/><span><strong>{dashboardSummary?.monitoreosHoy ?? '—'}</strong> monitoreos registrados hoy y <strong>{dashboardSummary?.monitoreosUltimos30Dias ?? '—'}</strong> en los últimos 30 días, según las mediciones registradas.</span></div>
          <div className="dashboard-grid"><section className="panel"><div className="panel-heading"><div><h2>Lecturas recientes</h2><p>Últimas mediciones registradas, con fecha, hora, estación y todos los parámetros capturados.</p></div><button className="text-link" onClick={() => navigate('estaciones')}>Ver estaciones <Icon name="logout" size={14}/></button></div>{records.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha y hora</th><th>Estación</th><th>pH</th><th>Turbidez (NTU)</th><th>Temperatura (°C)</th><th>Caudal (m³/s)</th><th>Observación</th></tr></thead><tbody>{records.slice(0, 8).map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.temperature}</td><td>{record.flow}</td><td>{record.observation || '—'}</td></tr>)}</tbody></table></div> : <EmptyState title={recordsLoadError ? 'No se pudo consultar el historial' : 'No hay lecturas registradas'} detail={recordsLoadError || 'Todavía no hay mediciones registradas. Cuando se guarde una lectura, aparecerá aquí y en el historial de estaciones.'}/>}</section>
          <section className="panel"><div className="panel-heading"><div><h2>Alertas recientes</h2><p>Alertas automáticas recientes y avisos publicados que todavía no has leído.</p></div><button className="icon-button" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell"/></button></div>{unreadPublishedAlertCount > 0 && <div className="inline-note"><Pill tone="danger">{unreadPublishedAlertCount} sin leer</Pill><span>Avisos publicados que esta cuenta todavía no ha marcado como vistos.</span></div>}{publishedAlerts.filter((item) => !item.leida).length > 0 && <div className="compact-alert-list">{publishedAlerts.filter((item) => !item.leida).map((item) => <article className="compact-alert" key={`pub-${item.idAlertaPublicada}`}><span className={`alert-symbol alert-${String(item.nivel || 'MEDIA').toLowerCase()}`}><Icon name="bell-ring" size={17}/></span><div><strong>{item.titulo}</strong><small>{item.area || 'Aviso general'} · {item.fechaCreacion ? new Date(item.fechaCreacion).toLocaleString('es-PE') : 'Fecha no indicada'}</small><small>{item.descripcion}</small></div><Pill tone="danger">No leída</Pill></article>)}</div>}{alerts.length > 0 && <><div className="eyebrow">EVENTOS AUTOMÁTICOS</div><div className="compact-alert-list">{alerts.slice(0, 4).map((item) => <article className="compact-alert" key={`auto-${item.id}`}><span className={`alert-symbol alert-${item.level.toLowerCase()}`}><Icon name="alert" size={17}/></span><div><strong>{item.title}</strong><small>{item.station} · {item.time}</small></div><Pill tone={['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) ? 'success' : item.level === 'Alta' ? 'danger' : item.level === 'Media' ? 'warning' : 'success'}>{item.level}</Pill></article>)}</div></>}{publishedAlerts.filter((item) => !item.leida).length === 0 && alerts.length === 0 && <EmptyState title="No hay alertas recientes" detail="No se han generado eventos por umbrales ni hay avisos pendientes. Esto no significa que falten mediciones: consulta los registros guardados en Lecturas recientes o en Estaciones → Historial de lecturas."/>}</section></div>
        </div>}

        {view === 'monitoreo' && isAdmin && <div className="page-stack"><SectionTitle kicker="OPERACIÓN / 02" title="Registro de monitoreo" description="Registra mediciones. El sistema valida los permisos, parámetros y umbrales antes de guardar los datos."/><section className="panel form-panel"><div className="panel-heading"><div><h2>Datos de la medición de la estación</h2><p>Registra una medición tomada en la estación seleccionada: fecha y hora de muestreo, pH, turbidez, temperatura, caudal y observaciones verificadas. Estos datos se guardan en el historial y se comparan con los umbrales activos.</p></div><Pill tone="info">Solo administrador</Pill></div><form className="form-body" onSubmit={submitMeasurement}><div className="form-grid"><div className="field field-span-2"><label htmlFor="measure-station">Estación *</label><select id="measure-station" name="station" required defaultValue={remotePoints.find((point) => point.estado === 'ACTIVO') ? `${remotePoints.find((point) => point.estado === 'ACTIVO').idPunto} · ${remotePoints.find((point) => point.estado === 'ACTIVO').nombrePunto}` : ''}>{(remotePoints || []).filter((station) => station.estado === 'ACTIVO').map((station) => <option key={station.idPunto} value={`${station.idPunto} · ${station.nombrePunto}`}>{station.idPunto} · {station.nombrePunto}</option>)}</select></div><div className="field"><label htmlFor="measure-date">Fecha *</label><input id="measure-date" name="date" type="date" required defaultValue={new Date().toISOString().slice(0, 10)}/></div><div className="field"><label htmlFor="measure-time">Hora *</label><input id="measure-time" name="time" type="time" required defaultValue="12:00"/></div><div className="field"><label htmlFor="measure-ph">pH *</label><input id="measure-ph" name="ph" type="number" step="0.01" min="0" max="14" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-turbidity">Turbidez (NTU) *</label><input id="measure-turbidity" name="turbidity" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-temperature">Temperatura (°C) *</label><input id="measure-temperature" name="temperature" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-flow">Caudal (m³/s) *</label><input id="measure-flow" name="flow" type="number" step="0.01" min="0" required placeholder="Valor medido"/></div><div className="field field-span-2"><label htmlFor="measure-observation">Observación</label><textarea id="measure-observation" name="observation" rows="3" placeholder="Describe solo observaciones verificadas"/></div></div><div className="form-footer"><span>La medición se guardará y aparecerá en el historial cuando se confirme la operación.</span><button className="button button-primary" type="submit"><Icon name="check" size={16}/> Validar y registrar</button></div></form></section><MonitoringEquipmentRegister notify={notify}/></div>}

        {view === 'estaciones' && <StationAndReadings isAdmin={isAdmin} points={remotePoints || []} records={records} recordsError={recordsLoadError} openAddReading={openAddReading} onAddReadingHandled={markReadingModalHandled} onRefresh={refreshStationsAndReadings} onEditReading={(record) => openEditor('record', record)} notify={notify}/> }

        {view === 'mapa2d' && <Map2D notify={notify} isAdmin={isAdmin}/>}

        {view === 'alertas' && <div className="page-stack"><SectionTitle kicker="SEGUIMIENTO / 05" title="Centro de alertas"/><PublishedAlerts isAdmin={isAdmin} notify={notify} onUnreadChange={handlePublishedUnreadChange} onAlertsChange={handlePublishedAlertsChange}/><OperationsPanel isAdmin={isAdmin} role={sessionUser?.rol || 'AUDITOR'} notify={notify}/></div>}

        {view === 'notificaciones' && <div className="page-stack">{isAdmin && <NotificationCenter notify={notify}/>}<UserNotifications onUnreadChange={handlePersonalNotificationCountChange} notify={notify}/></div>}

        {view === 'reportes' && <ExportCenter role={role} apiRole={sessionUser?.rol || ''} records={records} recordsError={recordsLoadError} stations={activeStations} alerts={alerts} audit={audit} reports={reports} onRefreshRecords={refreshStationsAndReadings} onEditReport={(report) => openEditor('report', report)} onEditRecord={(record) => openEditor('record', record)} notify={notify}/>}

        {view === 'usuarios' && isAdmin && <UserManagement notify={notify}/>}
        {view === 'configuracion' && <div className="page-stack configuration-page">{isAdmin && <UserManagement notify={notify}/>}<AccountSettings notify={notify} onProfileUpdated={handleProfileUpdated} isAdmin={isAdmin} onManageUsers={() => navigate('configuracion')}/></div>}

        <footer className="page-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>{dashboardSummary ? `Sistema actualizado · ${dashboardSummary.generadoEn ? new Date(dashboardSummary.generadoEn).toLocaleString('es-PE') : ''}` : 'Los datos estarán disponibles cuando el sistema complete la carga.'}</span></footer>
      </div>
    </main>

    {toast && <div className="toast-message" role="status"><Icon name="check" size={17}/><span>{toast}</span><button onClick={() => setToast('')} aria-label="Cerrar aviso"><Icon name="close" size={15}/></button></div>}

    {editor && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEditor(null); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="editor-title"><div className="modal-heading"><div><span className="eyebrow">CORRECCIÓN ADMINISTRATIVA</span><h2 id="editor-title">{editor.kind === 'report' ? `Corregir ${editor.id}` : `Corregir ${editor.id}`}</h2><p>{editor.kind === 'record' ? 'La corrección queda registrada y el sistema vuelve a evaluar las alertas.' : 'Los reportes ciudadanos persistentes se revisan en el Centro de alertas.'}</p></div><button className="icon-button" onClick={() => setEditor(null)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveEditor}>{editor.kind === 'report' ? <><div className="field"><label htmlFor="edit-report-category">Tipo de reporte</label><input id="edit-report-category" value={editorForm.category || ''} onChange={(event) => setEditorForm({ ...editorForm, category: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-title">Título</label><input id="edit-report-title" value={editorForm.title || ''} onChange={(event) => setEditorForm({ ...editorForm, title: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-area">Estación o zona</label><input id="edit-report-area" value={editorForm.area || ''} onChange={(event) => setEditorForm({ ...editorForm, area: event.target.value })}/></div><div className="field"><label htmlFor="edit-report-description">Descripción corregida</label><textarea id="edit-report-description" rows="5" value={editorForm.description || ''} onChange={(event) => setEditorForm({ ...editorForm, description: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-status">Estado</label><select id="edit-report-status" value={editorForm.status || 'Pendiente de revisión'} onChange={(event) => setEditorForm({ ...editorForm, status: event.target.value })}><option>Pendiente de revisión</option><option>En revisión</option><option>Atendido</option><option>Requiere información</option></select></div></> : <><div className="form-grid"><div className="field"><label htmlFor="edit-ph">pH</label><input id="edit-ph" type="number" step="0.01" min="0" max="14" value={editorForm.ph} onChange={(event) => setEditorForm({ ...editorForm, ph: event.target.value })} required/></div><div className="field"><label htmlFor="edit-turbidity">Turbidez (NTU)</label><input id="edit-turbidity" type="number" step="0.1" min="0" value={editorForm.turbidity} onChange={(event) => setEditorForm({ ...editorForm, turbidity: event.target.value })} required/></div><div className="field"><label htmlFor="edit-temp">Temperatura (°C)</label><input id="edit-temp" type="number" step="0.1" min="0" value={editorForm.temperature} onChange={(event) => setEditorForm({ ...editorForm, temperature: event.target.value })} required/></div><div className="field"><label htmlFor="edit-flow">Caudal (m³/s)</label><input id="edit-flow" type="number" step="0.01" min="0" value={editorForm.flow} onChange={(event) => setEditorForm({ ...editorForm, flow: event.target.value })} required/></div></div><div className="field"><label htmlFor="edit-observation">Observación</label><textarea id="edit-observation" rows="3" value={editorForm.observation} onChange={(event) => setEditorForm({ ...editorForm, observation: event.target.value })}/></div></>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setEditor(null)}>Cancelar</button><button type="submit" className="button button-primary" disabled={editorBusy}>{editorBusy ? 'Guardando…' : <><Icon name="check" size={16}/> Guardar corrección</>}</button></div></form></section></div>}
  </div>;
}

export default App;
