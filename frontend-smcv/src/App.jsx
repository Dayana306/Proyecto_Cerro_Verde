import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Icon from './components/Icon.jsx';
import { DemoNotice, EmptyState, MetricCard, Pill, SectionTitle } from './components/Shared.jsx';
import Map2D from './features/Map2D.jsx';
import MonitoringEquipmentRegister from './features/MonitoringEquipmentRegister.jsx';
import ExportCenter from './features/ExportCenter.jsx';
import OperationsPanel from './features/OperationsPanel.jsx';
import AccountSettings from './features/AccountSettings.jsx';
import UserManagement from './features/UserManagement.jsx';
import { initialAlerts, initialAudit, initialRecords, navigation, stations } from './data/prototypeData.js';
import { apiRequest, clearSession, currentUser, login as apiLogin, registerAccount } from './api.js';

function todayDisplay() {
  return new Date().toLocaleDateString('es-PE');
}

function toUiAlert(alert) {
  const level = String(alert.nivel || 'MEDIA').toUpperCase();
  const limit = alert.umbral?.limiteMax ?? alert.umbral?.limiteMin ?? alert.umbral?.criticoMax ?? alert.umbral?.criticoMin;
  return {
    id: String(alert.idAlerta ?? alert.id ?? ''),
    level: level.includes('ALTA') ? 'Alta' : 'Media',
    title: alert.parametro ? `${alert.parametro}: revisar umbral` : (alert.tipoAlerta || 'Alerta de monitoreo'),
    station: alert.punto || alert.nombrePunto || 'Punto de monitoreo',
    value: alert.valorDetectado != null ? String(alert.valorDetectado) : '—',
    limit: limit != null ? String(limit) : '—',
    time: alert.horaMuestreo || alert.fechaDeteccion || '',
    status: alert.estado || 'ABIERTA',
  };
}

function toUiRecord(record) {
  const readings = record.lecturas || [];
  const valueFor = (name) => readings.find((reading) => String(`${reading.parametro || ''} ${reading.codigo || ''} ${reading.nombreParametro || ''}`).toLowerCase().includes(name))?.valor ?? '—';
  const date = record.horaMuestreo ? new Date(record.horaMuestreo) : null;
  return {
    id: String(record.idMonitoreo ?? record.id ?? ''),
    station: record.punto || record.nombrePunto || 'Punto de monitoreo',
    date: date ? date.toLocaleDateString('es-PE') : '',
    time: date ? date.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '',
    ph: valueFor('ph') !== '—' ? valueFor('ph') : valueFor('hidrógeno'),
    turbidity: valueFor('turbidez'),
    temperature: valueFor('temperatura'),
    flow: valueFor('caudal') !== '—' ? valueFor('caudal') : valueFor('flujo'),
    observation: record.observacion || '',
  };
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
  return <div className="trend-chart-wrap"><div className="trend-chart-meta"><span>{values.length} días con mediciones</span><span>Unidad: {unit || '—'}</span></div><svg className="trend-chart" viewBox={`0 0 ${w} ${h}`} role="img" aria-label="Tendencia de lecturas reales del backend"><line x1={pad.l} y1={pad.t} x2={pad.l} y2={h-pad.b} className="chart-axis"/><line x1={pad.l} y1={h-pad.b} x2={w-pad.r} y2={h-pad.b} className="chart-axis"/>{[0,1,2,3].map((i)=><g key={i}><line x1={pad.l} y1={pad.t+i*(h-pad.t-pad.b)/3} x2={w-pad.r} y2={pad.t+i*(h-pad.t-pad.b)/3} className="chart-grid"/><text x={pad.l-8} y={pad.t+i*(h-pad.t-pad.b)/3+4} textAnchor="end" className="chart-label">{(max-i*(max-min)/3).toLocaleString('es-PE',{maximumFractionDigits:2})}</text></g>)}<polyline points={points} className="chart-line" fill="none" strokeLinecap="round" strokeLinejoin="round"/>{values.map((d,i)=><g key={d.fecha}><circle cx={x(i)} cy={y(Number(d.promedio))} r="3.5" className="chart-point"><title>{`${d.fecha}: ${d.promedio} ${unit || ''} (${d.mediciones} lecturas)`}</title></circle>{(values.length<=10 || i===0 || i===values.length-1 || i%Math.ceil(values.length/6)===0)&&<text x={x(i)} y={h-12} textAnchor="middle" className="chart-label">{dateLabel(d.fecha)}</text>}</g>)}</svg><div className="trend-chart-foot">Valores promedio diarios devueltos por <code>/api/dashboard/tendencia</code>. No se completan días sin mediciones con datos ficticios.</div></div>;
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
  const [remotePoints, setRemotePoints] = useState(null);
  const [entryScreen, setEntryScreen] = useState(() => new URLSearchParams(window.location.search).has('resetToken') ? 'reset-password' : 'welcome');
  const [passwordResetToken, setPasswordResetToken] = useState(() => new URLSearchParams(window.location.search).get('resetToken') || '');
  const [resetForm, setResetForm] = useState({ nueva: '', confirmar: '' });
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [registerForm, setRegisterForm] = useState({ nombre: '', apellido: '', correo: '', usuarioLogin: '', password: '', confirmarPassword: '', telefono: '' });
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');
  const [authNotice, setAuthNotice] = useState('');
  const [mobileNav, setMobileNav] = useState(false);
  const [view, setView] = useState('dashboard');
  const [search, setSearch] = useState('');
  const [alertFilter, setAlertFilter] = useState('Todas');
  const [alerts, setAlerts] = useState(() => initialAlerts.map((item) => ({ ...item })));
  const [records, setRecords] = useState(() => initialRecords.map((item) => ({ ...item })));
  const [reports, setReports] = useState([]);
  const [audit, setAudit] = useState(() => initialAudit.map((item, index) => ({ ...item, id: `AUD-DEMO-${index + 1}` })));
  const [toast, setToast] = useState('');
  const toastTimer = useRef(null);
  const [showThresholds, setShowThresholds] = useState(false);
  const [thresholdRows, setThresholdRows] = useState([]);
  const [thresholdParams, setThresholdParams] = useState([]);
  const [thresholdPoints, setThresholdPoints] = useState([]);
  const [thresholdForm, setThresholdForm] = useState({ idUmbral: '', idParametro: '', idPunto: '', limiteMin: '', limiteMax: '', criticoMin: '', criticoMax: '' });
  const [thresholdBusy, setThresholdBusy] = useState(false);
  const [thresholdError, setThresholdError] = useState('');
  const [editor, setEditor] = useState(null);
  const [editorForm, setEditorForm] = useState({});
  const [editorBusy, setEditorBusy] = useState(false);
  const roleLabel = (apiRole) => ({ ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', GERENTE: 'Gerente', AUDITOR: 'Auditor' }[apiRole] || 'Usuario');
  const roleForNavigation = (apiRole) => apiRole === 'ADMINISTRADOR' ? 'Administrador' : 'Usuario normal';

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
          apiRequest('/api/puntos-monitoreo?estado=ACTIVO'),
          apiRequest('/api/alertas?page=0&size=50'),
          apiRequest('/api/monitoreos?page=0&size=20&sort=horaMuestreo,desc'),
        ]);
        if (pointsResult.status === 'fulfilled' && Array.isArray(pointsResult.value)) setRemotePoints(pointsResult.value);
        if (alertsResult.status === 'fulfilled') { const list = alertsResult.value?.content || alertsResult.value; if (Array.isArray(list)) setAlerts(list.map(toUiAlert)); }
        if (recordsResult.status === 'fulfilled') { const list = recordsResult.value?.content || recordsResult.value; if (Array.isArray(list)) setRecords(list.map(toUiRecord)); }
      } catch { clearSession(); }
    };
    restore();
    const expire = () => { setAuthenticated(false); setSessionUser(null); setEntryScreen('login'); setAuthError('Tu sesión ha vencido. Inicia sesión nuevamente.'); };
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
    event.preventDefault(); setAuthBusy(true); setAuthError(''); setAuthNotice('');
    try {
      await apiRequest('/api/auth/password-reset/request', { method: 'POST', body: JSON.stringify({ correo: loginEmail.trim().toLowerCase() }) });
      setAuthNotice('Si el correo corresponde a una cuenta activa y el servidor de correo está configurado, recibirás un enlace válido durante 30 minutos. Revisa también la carpeta de spam.');
    } catch (error) { setAuthError(error.message || 'No se pudo procesar la solicitud.'); }
    finally { setAuthBusy(false); }
  };

  const handleConfirmPasswordReset = async (event) => {
    event.preventDefault(); setAuthError(''); setAuthNotice('');
    if (resetForm.nueva !== resetForm.confirmar) { setAuthError('Las contraseñas no coinciden.'); return; }
    if (resetForm.nueva.length < 8 || resetForm.nueva.length > 72) { setAuthError('La contraseña debe tener entre 8 y 72 caracteres.'); return; }
    setAuthBusy(true);
    try {
      await apiRequest('/api/auth/password-reset/confirm', { method: 'POST', body: JSON.stringify({ token: passwordResetToken, passwordNueva: resetForm.nueva }) });
      setLoginPassword(''); setResetForm({ nueva: '', confirmar: '' }); setPasswordResetToken('');
      window.history.replaceState({}, '', window.location.pathname);
      setEntryScreen('login'); setAuthNotice('Contraseña restablecida. Ya puedes iniciar sesión con tu nueva contraseña.');
    } catch (error) { setAuthError(error.message || 'El enlace no es válido o ya venció. Solicita uno nuevo.'); }
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
      setAlerts([]); setRecords([]); setRemotePoints([]);
      try {
        const [apiAlerts, apiPoints, apiRecords] = await Promise.all([
          apiRequest('/api/alertas?page=0&size=50'),
          apiRequest('/api/puntos-monitoreo?estado=ACTIVO'),
          apiRequest('/api/monitoreos?page=0&size=20&sort=horaMuestreo,desc'),
        ]);
        if (Array.isArray(apiAlerts?.content || apiAlerts)) setAlerts((apiAlerts?.content || apiAlerts).map(toUiAlert));
        if (Array.isArray(apiPoints)) setRemotePoints(apiPoints);
        if (Array.isArray(apiRecords?.content || apiRecords)) setRecords((apiRecords?.content || apiRecords).map(toUiRecord));
      } catch (loadError) { notify(`Sesión iniciada, pero algunos datos no se pudieron cargar: ${loadError.message}`); }
    } catch (error) { setAuthError(error.message || 'No se pudo iniciar sesión. Verifica tus credenciales y que el backend esté ejecutándose.'); }
    finally { setAuthBusy(false); }
  };

  const handleRegister = async (event) => {
    event.preventDefault();
    setAuthError(''); setAuthNotice('');
    if (registerForm.password !== registerForm.confirmarPassword) {
      setAuthError('Las contraseñas no coinciden.');
      return;
    }
    if (registerForm.password.length < 8) {
      setAuthError('La contraseña debe tener al menos 8 caracteres.');
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
      setEntryScreen('login');
      setAuthNotice('Tu cuenta se creó correctamente. Ahora puedes iniciar sesión con tu correo y contraseña.');
    } catch (error) {
      setAuthError(error.message || 'No se pudo crear la cuenta. Comprueba los datos y que el backend esté disponible.');
    } finally {
      setAuthBusy(false);
    }
  };

  const handleLogout = () => { clearSession(); setAuthenticated(false); setSessionUser(null); setProfilePhoto(''); setDashboardSummary(null); setTrendData(null); setDashboardParams([]); setTrendParameter(''); setView('dashboard'); setMobileNav(false); setEntryScreen('welcome'); };
  const isAdmin = role === 'Administrador';
  const handleProfileUpdated = useCallback((profile) => { setProfilePhoto(profile.fotoPerfil || ''); setDisplayName(`${profile.nombre || ''} ${profile.apellido || ''}`.trim() || profile.correo || ''); setSessionUser((old) => old ? { ...old, nombreCompleto: `${profile.nombre || ''} ${profile.apellido || ''}`.trim(), fotoPerfil: profile.fotoPerfil || '' } : old); }, []);
  const allowedIds = sessionUser?.rol === 'AUDITOR' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'reportes', 'configuracion'] : sessionUser?.rol === 'GERENTE' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'reportes', 'configuracion'] : sessionUser?.rol === 'SUPERVISOR' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'reportes', 'configuracion'] : null;
  const visibleNavigation = navigation.filter((item) => item.roles.includes(role) && (!allowedIds || allowedIds.includes(item.id)));
  const activeNavigation = visibleNavigation.find((item) => item.id === view) || visibleNavigation[0];

  const notify = (message) => {
    setToast(message);
    if (toastTimer.current) window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(''), 4200);
  };

  const openThresholds = async () => {
    if (!isAdmin) return;
    setShowThresholds(true); setThresholdBusy(true); setThresholdError('');
    try {
      const [thresholdResult, parameterResult, pointResult] = await Promise.all([
        apiRequest('/api/umbrales?estado=ACTIVO'),
        apiRequest('/api/parametros?estado=ACTIVO'),
        apiRequest('/api/puntos-monitoreo?estado=ACTIVO'),
      ]);
      const rows = Array.isArray(thresholdResult) ? thresholdResult : [];
      const params = Array.isArray(parameterResult) ? parameterResult : (parameterResult?.content || []);
      const points = Array.isArray(pointResult) ? pointResult : [];
      setThresholdRows(rows); setThresholdParams(params); setThresholdPoints(points);
      const first = params[0];
      setThresholdForm({ idUmbral: '', idParametro: first ? String(first.idParametro) : '', idPunto: '', limiteMin: '', limiteMax: '', criticoMin: '', criticoMax: '' });
    } catch (error) { setThresholdError(error.message || 'No se pudieron cargar los umbrales desde el backend.'); }
    finally { setThresholdBusy(false); }
  };

  const editThreshold = (row) => setThresholdForm({
    idUmbral: String(row.idUmbral), idParametro: String(row.idParametro), idPunto: row.idPunto == null ? '' : String(row.idPunto),
    limiteMin: row.limiteMin ?? '', limiteMax: row.limiteMax ?? '', criticoMin: row.criticoMin ?? '', criticoMax: row.criticoMax ?? '',
  });

  const saveThreshold = async (event) => {
    event.preventDefault();
    if (!isAdmin) return;
    const parse = (v) => v === '' || v == null ? null : Number(v);
    const body = { limiteMin: parse(thresholdForm.limiteMin), limiteMax: parse(thresholdForm.limiteMax), criticoMin: parse(thresholdForm.criticoMin), criticoMax: parse(thresholdForm.criticoMax) };
    const values = Object.values(body).filter(v => v != null);
    if (!thresholdForm.idParametro || !values.length || values.some(v => !Number.isFinite(v))) { setThresholdError('Selecciona un parámetro y completa al menos un límite numérico válido.'); return; }
    if (body.limiteMin != null && body.limiteMax != null && body.limiteMin > body.limiteMax) { setThresholdError('El límite mínimo no puede superar al máximo.'); return; }
    if (body.criticoMin != null && body.limiteMin != null && body.criticoMin > body.limiteMin) { setThresholdError('El mínimo crítico debe ser menor o igual al límite mínimo.'); return; }
    if (body.criticoMax != null && body.limiteMax != null && body.criticoMax < body.limiteMax) { setThresholdError('El máximo crítico debe ser mayor o igual al límite máximo.'); return; }
    setThresholdBusy(true); setThresholdError('');
    try {
      if (thresholdForm.idUmbral) {
        await apiRequest(`/api/umbrales/${thresholdForm.idUmbral}`, { method: 'PUT', body: JSON.stringify(body) });
      } else {
        await apiRequest('/api/umbrales', { method: 'POST', body: JSON.stringify({ ...body, idParametro: Number(thresholdForm.idParametro), idPunto: thresholdForm.idPunto ? Number(thresholdForm.idPunto) : null }) });
      }
      addAudit({ event: 'Umbral guardado', station: thresholdForm.idPunto ? `Punto ${thresholdForm.idPunto}` : 'General', detail: `Configuración de ${thresholdParams.find(p => String(p.idParametro) === String(thresholdForm.idParametro))?.nombreParametro || 'parámetro'} guardada en el backend`, status: 'Persistido', kind: 'warning' });
      notify('Umbral guardado en la base de datos.');
      const rows = await apiRequest('/api/umbrales?estado=ACTIVO');
      setThresholdRows(Array.isArray(rows) ? rows : []);
      setThresholdForm({ idUmbral: '', idParametro: thresholdForm.idParametro, idPunto: '', limiteMin: '', limiteMax: '', criticoMin: '', criticoMax: '' });
    } catch (error) { setThresholdError(error.message || 'No se pudo guardar el umbral.'); }
    finally { setThresholdBusy(false); }
  };

  const navigate = (next) => {
    const allowed = visibleNavigation.some((item) => item.id === next);
    if (!allowed) { notify('Tu perfil no tiene permiso para abrir esa sección.'); return; }
    setView(next);
    setMobileNav(false);
    setToast('');
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
      addAudit({ event:'Registro corregido', station:updated.station, detail:`${editor.id}: corrección persistida por el administrador; el backend conserva el antes/después en auditoría`, status:'Registrado', kind:'warning' });
      try {
        const alertResult = await apiRequest('/api/alertas?page=0&size=50');
        const alertList = alertResult?.content || alertResult;
        if (Array.isArray(alertList)) setAlerts(alertList.map(toUiAlert));
      } catch { /* La corrección ya se guardó; la actualización visual de alertas puede reintentarse. */ }
      notify('Corrección guardada en MariaDB; Spring Boot registró el cambio y reevaluó las alertas.');
      setEditor(null);
    } catch (error) {
      notify(`No se guardó la corrección: ${error.message || 'error de backend'}`);
    } finally { setEditorBusy(false); }
  };

  const sendReport = (form) => {
    const report = { id: `REP-${String(reports.length + 1).padStart(3, '0')}`, date: todayDisplay(), ...form, status: 'Pendiente de revisión' };
    setReports((current) => [report, ...current]);
    addAudit({ event: 'Reporte enviado', station: form.area || 'No indicada', detail: `${report.id}: ${form.title}`, status: 'Pendiente', kind: 'success' });
    notify('Reporte añadido a la sesión. El administrador podrá revisarlo al ingresar a esta misma página.');
    setView('reportes');
  };

  const acknowledgeAlert = async (id) => {
    if (!isAdmin) return;
    const item = alerts.find((alert) => alert.id === id);
    try {
      await apiRequest(`/api/alertas/${id}/seguimientos`, { method: 'POST', body: JSON.stringify({ comentario: 'Alerta revisada por el administrador desde la interfaz.', nuevoEstado: 'CERRADA' }) });
      setAlerts((current) => current.map((alert) => alert.id === id ? { ...alert, status: 'CERRADA' } : alert));
      addAudit({ event: 'Seguimiento de alerta registrado', station: item?.station || '', detail: `${id}: seguimiento guardado por el administrador`, status: 'CERRADA', kind: 'success' });
      notify('Seguimiento guardado en el backend.');
    } catch (error) { notify(`No se pudo guardar el seguimiento: ${error.message}`); }
  };

  const submitMeasurement = async (event) => {
    event.preventDefault();
    if (!isAdmin) return;
    const formData = new FormData(event.currentTarget);
    const value = Object.fromEntries(formData.entries());
    const numbers = { ph: Number(value.ph), turbidity: Number(value.turbidity), temperature: Number(value.temperature), flow: Number(value.flow) };
    if (Object.values(numbers).some((number) => !Number.isFinite(number) || number < 0) || numbers.ph > 14) { notify('Verifica los parámetros ingresados.'); return; }
    try {
      const points = remotePoints || await apiRequest('/api/puntos-monitoreo?estado=ACTIVO');
      const pointId = Number(String(value.station).split(' · ')[0]);
      const selectedPoint = points.find((point) => Number(point.idPunto) === pointId);
      if (!selectedPoint) throw new Error('No se encontró el punto seleccionado en la base de datos. Actualiza la pantalla e inténtalo nuevamente.');
      const parameters = await apiRequest('/api/parametros?estado=ACTIVO');
      const parameterList = Array.isArray(parameters) ? parameters : (parameters.content || []);
      const matchParam = (patterns) => parameterList.find((p) => patterns.some((pattern) => String(p.nombreParametro || p.nombre || '').toLowerCase().includes(pattern)));
      const pH = matchParam(['ph', 'potencial de hidrógeno']);
      const turb = matchParam(['turbidez']);
      const temp = matchParam(['temperatura']);
      const flow = matchParam(['caudal', 'flujo']);
      if ([pH, turb, temp, flow].some((p) => !p?.idParametro)) throw new Error('La API no devolvió los cuatro parámetros necesarios (pH, turbidez, temperatura y caudal). No se guardó la medición.');
      const saved = await apiRequest('/api/monitoreos', { method: 'POST', body: JSON.stringify({ idPunto: pointId, tipoMuestreo: 'MANUAL', horaMuestreo: `${value.date}T${value.time}:00`, observacion: value.observation || '', lecturas: [{ idParametro: pH.idParametro, valor: numbers.ph }, { idParametro: turb.idParametro, valor: numbers.turbidity }, { idParametro: temp.idParametro, valor: numbers.temperature }, { idParametro: flow.idParametro, valor: numbers.flow }] }) });
      const record = { id: String(saved.idMonitoreo ?? saved.id ?? 'Guardado'), station: selectedPoint.nombrePunto, date: value.date.split('-').reverse().join('/'), time: value.time, ...numbers, observation: value.observation || 'Sin observaciones' };
      setRecords((current) => [record, ...current]);
      if (saved.alertas?.length) setAlerts((current) => [...saved.alertas.map((a) => ({ id: String(a.idAlerta ?? a.id ?? ''), level: String(a.nivel || 'MEDIA').includes('ALTA') ? 'Alta' : 'Media', title: a.descripcion || a.mensaje || 'Alerta generada por el backend', station: selectedPoint.nombrePunto, value: 'Ver detalle en el sistema', limit: 'Según umbral configurado', time: `${record.date} · ${record.time}`, status: a.estado || 'ABIERTA' })), ...current]);
      notify('Medición guardada en el backend y enviada a la base de datos.');
      event.currentTarget.reset();
    } catch (error) {
      notify(`No se guardó la medición: ${error.message}. Verifica la API, los permisos y los catálogos de la base de datos.`);
      return;
    }
  };


  const matchingIndicator = (patterns) => (dashboardSummary?.indicadores || []).find((indicator) => patterns.some((pattern) => `${indicator.parametro || ''} ${indicator.codigo || ''}`.toLowerCase().includes(pattern)));
  const formatIndicator = (patterns) => { const indicator = matchingIndicator(patterns); return indicator?.ultimoValor == null ? '—' : Number(indicator.ultimoValor).toLocaleString('es-PE', { maximumFractionDigits: 3 }); };
  const indicatorUnit = (patterns, fallback) => matchingIndicator(patterns)?.unidadMedida || fallback;
  const indicatorDetail = (patterns) => { const i = matchingIndicator(patterns); if (!i) return dashboardSummary ? 'Sin lecturas registradas' : 'Esperando respuesta de la API'; const when = i.fecha ? new Date(i.fecha).toLocaleString('es-PE') : ''; return `${i.punto || 'Punto no indicado'}${when ? ` · ${when}` : ''}`; };
  const activeStations = useMemo(() => remotePoints ? remotePoints.map((point) => { const record = records.find((r) => r.station === point.nombrePunto); return { id: String(point.idPunto), name: point.nombrePunto, area: point.area || '—', ph: record?.ph ?? '—', turbidity: record?.turbidity ?? '—', flow: record?.flow ?? '—', temperature: record?.temperature ?? '—', status: point.estado || 'Activo' }; }) : stations, [remotePoints, records]);
  const filteredStations = useMemo(() => activeStations.filter((station) => `${station.id} ${station.name} ${station.area}`.toLowerCase().includes(search.toLowerCase())), [search, activeStations]);
  const filteredAlerts = useMemo(() => alerts.filter((item) => alertFilter === 'Todas' || (alertFilter === 'Abiertas' ? !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) : ['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()))), [alerts, alertFilter]);
  const pendingAlerts = alerts.filter((item) => !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length;

  if (!authenticated) return <main className="welcome-screen">
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
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? 'Verificando…' : 'Entrar al sistema'} <Icon name="arrow-right" size={17}/></button>
    </form>
    <button type="button" className="text-link forgot-link" onClick={() => { setEntryScreen('forgot'); setAuthError(''); setAuthNotice(''); }}>Olvidé mi contraseña</button>
    <div className="login-help"><Icon name="info" size={16}/><span>La API debe estar disponible en <code>{import.meta.env.VITE_API_URL || 'http://localhost:8081'}</code>. La cuenta de administrador de prueba documentada en el backend es <strong>admin@smcv.local</strong>.</span></div>
  </> : entryScreen === 'forgot' ? <>
    <button className="back-link" onClick={() => { setEntryScreen('login'); setAuthError(''); setAuthNotice(''); }}><Icon name="arrow-left" size={15}/> Volver al inicio de sesión</button>
    <span className="eyebrow">RECUPERACIÓN DE CUENTA</span><h2>Olvidé mi contraseña</h2><p>Escribe el correo de tu cuenta. Te enviaremos un enlace para crear una contraseña nueva.</p>
    <form className="auth-form" onSubmit={handleForgotPassword}><div className="field"><label htmlFor="forgot-email">Correo electrónico</label><input id="forgot-email" type="email" autoComplete="email" value={loginEmail} onChange={(e)=>setLoginEmail(e.target.value)} placeholder="nombre@dominio.com" required/></div>
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}{authNotice && <div className="auth-success" role="status"><Icon name="check" size={16}/>{authNotice}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy?'Enviando solicitud…':'Solicitar enlace'} <Icon name="send" size={16}/></button></form>
    <div className="login-help"><Icon name="shield-check" size={16}/><span>Por seguridad no permitimos cambiar una contraseña indicando solo el correo. El enlace es de un solo uso y vence en 30 minutos.</span></div>
  </> : entryScreen === 'reset-password' ? <>
    <span className="eyebrow">NUEVA CONTRASEÑA</span><h2>Restablecer contraseña</h2><p>Define la nueva contraseña y confírmala. El enlace debe seguir vigente y no haber sido utilizado.</p>
    <form className="auth-form" onSubmit={handleConfirmPasswordReset}><div className="field"><label htmlFor="reset-new">Nueva contraseña</label><input id="reset-new" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={resetForm.nueva} onChange={(e)=>setResetForm({...resetForm,nueva:e.target.value})} required/></div><div className="field"><label htmlFor="reset-confirm">Confirmar nueva contraseña</label><input id="reset-confirm" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={resetForm.confirmar} onChange={(e)=>setResetForm({...resetForm,confirmar:e.target.value})} required/></div>
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy?'Restableciendo…':'Guardar nueva contraseña'} <Icon name="check" size={16}/></button></form>
    <div className="login-help"><Icon name="lock" size={16}/><span>Si el enlace venció, vuelve al inicio de sesión y solicita uno nuevo.</span></div>
  </> : <>
    <button className="back-link" onClick={() => { setEntryScreen('welcome'); setAuthError(''); setAuthNotice(''); }}><Icon name="arrow-left" size={15}/> Volver</button>
    <span className="eyebrow">CREACIÓN DE CUENTAS</span>
    <h2>Crear cuenta</h2>
    <p>Completa tus datos para registrar una cuenta en el sistema. Los campos con * son obligatorios.</p>
    <form className="auth-form register-form" onSubmit={handleRegister}>
      <div className="field"><label htmlFor="register-nombre">Nombres *</label><input id="register-nombre" autoComplete="given-name" maxLength="80" value={registerForm.nombre} onChange={(e) => setRegisterForm({ ...registerForm, nombre: e.target.value })} placeholder="Tus nombres" required/></div>
      <div className="field"><label htmlFor="register-apellido">Apellidos *</label><input id="register-apellido" autoComplete="family-name" maxLength="80" value={registerForm.apellido} onChange={(e) => setRegisterForm({ ...registerForm, apellido: e.target.value })} placeholder="Tus apellidos" required/></div>
      <div className="field"><label htmlFor="register-correo">Correo electrónico *</label><input id="register-correo" type="email" autoComplete="email" maxLength="120" value={registerForm.correo} onChange={(e) => setRegisterForm({ ...registerForm, correo: e.target.value })} placeholder="nombre@dominio.com" required/></div>
      <div className="field"><label htmlFor="register-usuario">Nombre de usuario *</label><input id="register-usuario" autoComplete="username" minLength="3" maxLength="40" value={registerForm.usuarioLogin} onChange={(e) => setRegisterForm({ ...registerForm, usuarioLogin: e.target.value })} placeholder="Ej. maria.lopez" required/></div>
      <div className="field"><label htmlFor="register-telefono">Teléfono (opcional)</label><input id="register-telefono" type="tel" autoComplete="tel" maxLength="20" value={registerForm.telefono} onChange={(e) => setRegisterForm({ ...registerForm, telefono: e.target.value })} placeholder="Tu teléfono"/></div>
      <div className="field"><label htmlFor="register-password">Contraseña *</label><input id="register-password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={registerForm.password} onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })} placeholder="Mínimo 8 caracteres" required/></div>
      <div className="field"><label htmlFor="register-confirm-password">Confirmar contraseña *</label><input id="register-confirm-password" type="password" autoComplete="new-password" minLength="8" maxLength="72" value={registerForm.confirmarPassword} onChange={(e) => setRegisterForm({ ...registerForm, confirmarPassword: e.target.value })} placeholder="Repite la contraseña" required/></div>
      {authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}
      <button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? 'Creando cuenta…' : 'Crear cuenta'} <Icon name="user-plus" size={17}/></button>
    </form>
    <div className="login-help"><Icon name="shield-check" size={16}/><span>La contraseña se protege en el servidor. Las cuentas nuevas reciben permisos de auditoría y no pueden administrar usuarios ni cambiar roles.</span></div>
  </>}
</section></div><footer className="welcome-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>Conexión mediante API segura</span></footer>
  </main>;

  return <div className={`app-shell ${mobileNav ? 'nav-open' : ''}`}>
    {mobileNav && <button className="mobile-scrim" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"/>}
    <aside className="sidebar"><div className="brand sidebar-brand"><span className="brand-mark"><Icon name="droplet" size={23}/></span><span><strong>CERRO VERDE</strong><small>MONITOREO HÍDRICO</small></span><button className="mobile-close" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"><Icon name="close"/></button></div>
      <div className="sidebar-project"><span className="sidebar-project-dot"/><div><strong>Área de trabajo</strong><small>Panel ambiental</small></div></div><div className="nav-label">MENÚ PRINCIPAL</div>
      <nav className="sidebar-nav" aria-label="Navegación principal">{visibleNavigation.map((item) => <button key={item.id} className={`nav-link ${view === item.id ? 'nav-link-active' : ''}`} onClick={() => navigate(item.id)}><Icon name={item.icon} size={19}/><span>{item.label}</span>{item.id === 'alertas' && pendingAlerts > 0 && <small className="nav-count">{pendingAlerts}</small>}</button>)}</nav>
      <div className="sidebar-account"><div className="avatar avatar-photo">{profilePhoto ? <img src={profilePhoto} alt="Foto de perfil"/> : (isAdmin ? 'A' : 'U')}</div><div className="account-meta"><strong>Mi perfil</strong><small>{displayName || (isAdmin ? 'Administrador' : 'Usuario normal')}</small></div></div>
      <button type="button" className="logout-button" onClick={handleLogout} aria-label="Cerrar sesión"><Icon name="logout" size={18}/><span>Cerrar sesión</span></button>
    </aside>
    <main className="main-column"><header className="topbar"><div className="topbar-heading"><button className="mobile-menu" onClick={() => setMobileNav(true)} aria-label="Abrir menú"><Icon name="menu" size={22}/></button><div><span>Panel de control</span><strong>{activeNavigation?.label || 'Resumen general'}</strong></div></div><div className="topbar-right"><span className="demo-status"><i/>Conectado como {roleLabel(sessionUser?.rol)}</span><button className="topbar-alert" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell" size={19}/>{pendingAlerts > 0 && <i/>}</button><span className="topbar-separator"/><div className="topbar-profile"><span className="avatar avatar-small avatar-photo">{profilePhoto ? <img src={profilePhoto} alt="Foto de perfil"/> : (isAdmin ? 'A' : 'U')}</span><div><strong>{displayName || (isAdmin ? 'Administrador' : 'Usuario normal')}</strong><small>{isAdmin ? 'Administrador' : 'Usuario normal'}</small></div></div></div></header>
      <div className="content-area">
        {view === 'dashboard' && <div className="page-stack"><SectionTitle kicker="VISTA GENERAL / 01" title="Resumen del monitoreo" description="Resumen de monitoreo del sistema; los datos consultados se obtienen desde la API cuando está disponible." action={isAdmin ? <button className="button button-primary" onClick={() => navigate('monitoreo')}><Icon name="plus" size={16}/> Registrar medición</button> : <button className="button button-primary" onClick={() => navigate('alertas')}><Icon name="send" size={16}/> Enviar observación</button>}/><div className="metrics-grid"><MetricCard label="Puntos de monitoreo activos" value={String(dashboardSummary?.puntosActivos ?? '—')} unit="puntos" detail={dashboardSummary ? 'Conteo del backend' : 'Esperando respuesta de la API'} icon="pin" tone="teal"/><MetricCard label="Última lectura de pH" value={formatIndicator(['ph', 'hidrógeno'])} unit={indicatorUnit(['ph', 'hidrógeno'], 'pH')} detail={indicatorDetail(['ph', 'hidrógeno'])} icon="droplet" tone="blue"/><MetricCard label="Última lectura de turbidez" value={formatIndicator(['turbidez'])} unit={indicatorUnit(['turbidez'], 'NTU')} detail={indicatorDetail(['turbidez'])} icon="activity" tone="green"/><MetricCard label="Última lectura de caudal" value={formatIndicator(['caudal', 'flujo'])} unit={indicatorUnit(['caudal', 'flujo'], 'm³/s')} detail={indicatorDetail(['caudal', 'flujo'])} icon="activity" tone="amber"/><MetricCard label="Alertas no cerradas" value={String(dashboardSummary?.alertas?.noCerradas ?? '—')} unit="eventos" detail={dashboardSummary ? `${dashboardSummary.alertas?.alta || 0} de prioridad alta` : 'Esperando respuesta de la API'} icon="bell" tone="red"/></div>
        <section className="panel dashboard-trend-panel"><div className="panel-heading"><div><h2>Tendencia de monitoreo</h2><p>Promedios diarios y unidad de medida consultados al backend.</p></div><label className="trend-parameter-picker"><span>Parámetro</span><select value={trendParameter} onChange={(e)=>{setTrendError('');setTrendParameter(e.target.value);}} disabled={!dashboardParams.length}>{dashboardParams.map((param)=> <option key={param.idParametro} value={param.idParametro}>{param.nombreParametro || param.nombre}</option>)}</select></label></div>{trendError && <div className="auth-error" role="alert">{trendError}</div>}{trendData && <div className="trend-summary"><div><small>Parámetro</small><strong>{trendData.parametro}</strong></div><div><small>Desde</small><strong>{trendData.desde}</strong></div><div><small>Hasta</small><strong>{trendData.hasta}</strong></div></div>}<TrendChart data={trendData} unit={trendData?.unidadMedida}/></section>
        <div className="dashboard-live-info"><Icon name="database" size={18}/><span><strong>{dashboardSummary?.monitoreosHoy ?? '—'}</strong> monitoreos registrados hoy y <strong>{dashboardSummary?.monitoreosUltimos30Dias ?? '—'}</strong> en los últimos 30 días, según la API.</span></div>
          <div className="dashboard-grid"><section className="panel"><div className="panel-heading"><div><h2>Lecturas recientes</h2><p>Registros disponibles en esta sesión</p></div><button className="text-link" onClick={() => navigate('estaciones')}>Ver estaciones <Icon name="logout" size={14}/></button></div>{records.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Caudal</th></tr></thead><tbody>{records.slice(0, 4).map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.flow}</td></tr>)}</tbody></table></div> : <EmptyState/>}</section>
          <section className="panel"><div className="panel-heading"><div><h2>Alertas recientes</h2><p>Eventos que requieren consulta</p></div><button className="icon-button" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell"/></button></div><div className="compact-alert-list">{alerts.slice(0, 4).map((item) => <article className="compact-alert" key={item.id}><span className={`alert-symbol alert-${item.level.toLowerCase()}`}><Icon name="alert" size={17}/></span><div><strong>{item.title}</strong><small>{item.station} · {item.time}</small></div><Pill tone={['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) ? 'success' : item.level === 'Alta' ? 'danger' : 'warning'}>{item.level}</Pill></article>)}</div></section></div>
        </div>}

        {view === 'monitoreo' && isAdmin && <div className="page-stack"><SectionTitle kicker="OPERACIÓN / 02" title="Registro de monitoreo" description="Registra mediciones en el backend. El servidor valida permisos, parámetros y umbrales antes de persistir los datos."/><section className="panel form-panel"><div className="panel-heading"><div><h2>Datos de la medición</h2><p>Ingresa los valores tomados de una fuente verificada.</p></div><Pill tone="info">Solo administrador</Pill></div><form className="form-body" onSubmit={submitMeasurement}><div className="form-grid"><div className="field field-span-2"><label htmlFor="measure-station">Estación *</label><select id="measure-station" name="station" required defaultValue={remotePoints?.[0]?.idPunto ? `${remotePoints[0].idPunto} · ${remotePoints[0].nombrePunto}` : `${stations[0].id} · ${stations[0].name}`}>{(remotePoints || stations).map((station) => <option key={station.idPunto ?? station.id} value={`${station.idPunto ?? station.id} · ${station.nombrePunto ?? station.name}`}>{station.idPunto ?? station.id} · {station.nombrePunto ?? station.name}</option>)}</select></div><div className="field"><label htmlFor="measure-date">Fecha *</label><input id="measure-date" name="date" type="date" required defaultValue={new Date().toISOString().slice(0, 10)}/></div><div className="field"><label htmlFor="measure-time">Hora *</label><input id="measure-time" name="time" type="time" required defaultValue="12:00"/></div><div className="field"><label htmlFor="measure-ph">pH *</label><input id="measure-ph" name="ph" type="number" step="0.01" min="0" max="14" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-turbidity">Turbidez (NTU) *</label><input id="measure-turbidity" name="turbidity" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-temperature">Temperatura (°C) *</label><input id="measure-temperature" name="temperature" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-flow">Caudal (m³/s) *</label><input id="measure-flow" name="flow" type="number" step="0.01" min="0" required placeholder="Valor medido"/></div><div className="field field-span-2"><label htmlFor="measure-observation">Observación</label><textarea id="measure-observation" name="observation" rows="3" placeholder="Describe solo observaciones verificadas"/></div></div><div className="form-footer"><span>La medición se enviará al backend y solo aparecerá como guardada si el servidor confirma la operación.</span><button className="button button-primary" type="submit"><Icon name="check" size={16}/> Validar y registrar</button></div></form></section><MonitoringEquipmentRegister notify={notify}/></div>}

        {view === 'estaciones' && <div className="page-stack"><SectionTitle kicker="CONSULTA / 03" title="Estaciones de monitoreo" description="Consulta el catálogo visual disponible y las lecturas precargadas."/><DemoNotice/><section className="panel"><div className="panel-heading"><div><h2>Catálogo de estaciones</h2><p>El catálogo se consulta en la API; las lecturas requieren el historial de monitoreos correspondiente.</p></div><div className="search-control"><Icon name="search" size={16}/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar estación..." aria-label="Buscar estación"/></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Estación</th><th>Área</th><th>pH</th><th>Turbidez (NTU)</th><th>Caudal (m³/s)</th><th>Temperatura (°C)</th><th>Estado indicado</th></tr></thead><tbody>{filteredStations.map((station) => <tr key={station.id}><td><strong>{station.id}</strong></td><td>{station.name}</td><td>{station.area}</td><td>{station.ph}</td><td>{station.turbidity}</td><td>{station.flow}</td><td>{station.temperature}</td><td><Pill tone={['Conforme', 'ACTIVO', 'Activo'].includes(station.status) ? 'success' : 'warning'}>{station.status}</Pill></td></tr>)}</tbody></table></div></section><section className="panel"><div className="panel-heading"><div><h2>Historial de lecturas</h2><p>Registros heredados y añadidos durante esta sesión</p></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha y hora</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Temperatura</th><th>Caudal</th><th>Observación</th>{isAdmin && <th>Corrección</th>}</tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.temperature}</td><td>{record.flow}</td><td>{record.observation}</td>{isAdmin && <td><button className="button button-secondary button-small" onClick={() => openEditor('record', record)}><Icon name="edit" size={14}/> Corregir</button></td>}</tr>)}</tbody></table></div></section></div>}

        {view === 'mapa2d' && <Map2D notify={notify} isAdmin={isAdmin}/>}

        {view === 'alertas' && <div className="page-stack"><SectionTitle kicker="SEGUIMIENTO / 05" title="Centro de alertas" description={isAdmin ? 'Gestiona umbrales, reportes recibidos, equipos, personal operativo y mantenimiento en un solo centro.' : 'Consulta alertas y reporta observaciones ambientales o posibles riesgos de tu zona.'} action={isAdmin && <button className="button button-secondary" onClick={openThresholds}><Icon name="shield" size={16}/> Configurar umbrales</button>}/><DemoNotice/><div className="alert-summary-grid"><article><span className="alert-tile-icon red"><Icon name="alert"/></span><strong>{alerts.filter((item) => item.level === 'Alta' && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Prioridad alta abierta</small></article><article><span className="alert-tile-icon amber"><Icon name="activity"/></span><strong>{alerts.filter((item) => item.level === 'Media' && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Prioridad media abierta</small></article><article><span className="alert-tile-icon green"><Icon name="check"/></span><strong>{alerts.filter((item) => ['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Atendidas en sesión</small></article></div><section className="panel"><div className="panel-heading"><div><h2>Eventos visibles</h2><p>Las alertas se consultan desde la API; los cambios de estado se guardan con seguimiento.</p></div><div className="filter-tabs">{['Todas', 'Abiertas', 'Atendidas'].map((filter) => <button key={filter} className={alertFilter === filter ? 'filter-active' : ''} onClick={() => setAlertFilter(filter)}>{filter}</button>)}</div></div>{filteredAlerts.length ? <div className="alerts-list">{filteredAlerts.map((item) => <article className="alert-row" key={item.id}><span className={`alert-symbol alert-${item.level.toLowerCase()}`}><Icon name="alert" size={19}/></span><div className="alert-main"><div className="alert-title-row"><strong>{item.title}</strong><Pill tone={item.level === 'Alta' ? 'danger' : 'warning'}>{item.level}</Pill></div><p>{item.station} · {item.time}</p><div className="alert-value-line"><span>Valor indicado <strong>{item.value}</strong></span><span>Referencia indicada <strong>{item.limit}</strong></span></div></div><div className="alert-side"><Pill tone={['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) ? 'success' : 'neutral'}>{item.status}</Pill>{isAdmin && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) && <button className="button button-secondary button-small" onClick={() => acknowledgeAlert(item.id)}><Icon name="check" size={14}/> Marcar atendida</button>}</div></article>)}</div> : <EmptyState title="No hay alertas en este filtro" detail="Prueba otra opción de filtro."/>}<p className="inline-note"><Icon name="info" size={15}/> Las alertas de lecturas dependen de los parámetros, umbrales y mediciones persistidos. No se simula la conexión de instrumentos que no estén integrados.</p></section><OperationsPanel isAdmin={isAdmin} role={sessionUser?.rol || 'AUDITOR'} notify={notify}/></div>}

                {view === 'reportes' && <ExportCenter role={role} records={records} stations={stations} alerts={alerts} audit={audit} reports={reports} onEditReport={(report) => openEditor('report', report)} onEditRecord={(record) => openEditor('record', record)} notify={notify}/>}

        {view === 'usuarios' && isAdmin && <UserManagement notify={notify}/>}
        {view === 'configuracion' && <AccountSettings notify={notify} onProfileUpdated={handleProfileUpdated} isAdmin={false} onManageUsers={() => navigate('usuarios')}/>}

        <footer className="page-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>{dashboardSummary ? `Conectado a API · actualizado ${dashboardSummary.generadoEn ? new Date(dashboardSummary.generadoEn).toLocaleString('es-PE') : ''}` : 'Los datos se muestran cuando la API responde.'}</span></footer>
      </div>
    </main>

    {toast && <div className="toast-message" role="status"><Icon name="check" size={17}/><span>{toast}</span><button onClick={() => setToast('')} aria-label="Cerrar aviso"><Icon name="close" size={15}/></button></div>}

    {showThresholds && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowThresholds(false); }}><section className="modal-card threshold-manager-modal" role="dialog" aria-modal="true" aria-labelledby="threshold-title"><div className="modal-heading"><div><span className="eyebrow">CENTRO DE ALERTAS / CONFIGURACIÓN DE LÍMITES</span><h2 id="threshold-title">Umbrales hídricos</h2><p>Los cambios se guardan en MariaDB y se versionan en el backend. Selecciona un límite existente para editarlo o crea uno nuevo.</p></div><button className="icon-button" onClick={() => setShowThresholds(false)} aria-label="Cerrar"><Icon name="close"/></button></div>{thresholdError && <div className="auth-error" role="alert">{thresholdError}</div>}{thresholdBusy && <div className="loading-inline"><Icon name="loader-circle" size={18}/> Consultando/guardando umbrales…</div>}<div className="threshold-current-list">{thresholdRows.length ? thresholdRows.map(row=><button key={row.idUmbral} type="button" className={`threshold-current-row ${String(thresholdForm.idUmbral)===String(row.idUmbral)?'threshold-current-row-active':''}`} onClick={()=>editThreshold(row)}><span><strong>{row.parametro || `Parámetro #${row.idParametro}`}</strong><small>{row.punto || 'General (todos los puntos)'}</small></span><span><small>Normal</small><strong>{row.limiteMin ?? '—'} a {row.limiteMax ?? '—'}</strong></span><span><small>Crítico</small><strong>{row.criticoMin ?? '—'} a {row.criticoMax ?? '—'}</strong></span><Pill tone="success">Activo</Pill></button>) : !thresholdBusy && <EmptyState title="No hay umbrales activos" detail="Crea uno nuevo para que las mediciones puedan generar alertas según esos límites."/>}</div><form className="form-body threshold-edit-form" onSubmit={saveThreshold}><div className="panel-heading"><div><h3>{thresholdForm.idUmbral ? `Editar umbral #${thresholdForm.idUmbral}` : 'Crear umbral'}</h3><p>Los límites críticos deben estar en el exterior del rango normal.</p></div><button type="button" className="button button-secondary button-small" onClick={()=>setThresholdForm({idUmbral:'',idParametro:thresholdParams[0]?.idParametro ? String(thresholdParams[0].idParametro) : '',idPunto:'',limiteMin:'',limiteMax:'',criticoMin:'',criticoMax:''})}><Icon name="plus" size={14}/> Nuevo</button></div><div className="form-grid"><div className="field"><label>Parámetro *</label><select required value={thresholdForm.idParametro} disabled={Boolean(thresholdForm.idUmbral)} onChange={e=>setThresholdForm({...thresholdForm,idParametro:e.target.value})}><option value="">Seleccionar parámetro</option>{thresholdParams.map(p=><option key={p.idParametro} value={p.idParametro}>{p.nombreParametro} ({p.unidadMedida})</option>)}</select></div><div className="field"><label>Punto de monitoreo</label><select disabled={Boolean(thresholdForm.idUmbral)} value={thresholdForm.idPunto} onChange={e=>setThresholdForm({...thresholdForm,idPunto:e.target.value})}><option value="">General (todos los puntos)</option>{thresholdPoints.map(p=><option key={p.idPunto} value={p.idPunto}>{p.nombrePunto}</option>)}</select></div><div className="field"><label>Límite mínimo</label><input type="number" step="any" value={thresholdForm.limiteMin} onChange={e=>setThresholdForm({...thresholdForm,limiteMin:e.target.value})} placeholder="Opcional"/></div><div className="field"><label>Límite máximo</label><input type="number" step="any" value={thresholdForm.limiteMax} onChange={e=>setThresholdForm({...thresholdForm,limiteMax:e.target.value})} placeholder="Opcional"/></div><div className="field"><label>Mínimo crítico</label><input type="number" step="any" value={thresholdForm.criticoMin} onChange={e=>setThresholdForm({...thresholdForm,criticoMin:e.target.value})} placeholder="Opcional"/></div><div className="field"><label>Máximo crítico</label><input type="number" step="any" value={thresholdForm.criticoMax} onChange={e=>setThresholdForm({...thresholdForm,criticoMax:e.target.value})} placeholder="Opcional"/></div></div><div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setShowThresholds(false)}>Cerrar</button><button type="submit" className="button button-primary" disabled={thresholdBusy}><Icon name="check" size={16}/>{thresholdBusy?'Guardando…':'Guardar en la base de datos'}</button></div></form></section></div>}

    {editor && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEditor(null); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="editor-title"><div className="modal-heading"><div><span className="eyebrow">CORRECCIÓN ADMINISTRATIVA</span><h2 id="editor-title">{editor.kind === 'report' ? `Corregir ${editor.id}` : `Corregir ${editor.id}`}</h2><p>{editor.kind === 'record' ? 'La corrección se guarda en la base de datos y el backend conserva la trazabilidad del cambio y reevalúa las alertas.' : 'Los reportes ciudadanos persistentes se revisan en el Centro de alertas.'}</p></div><button className="icon-button" onClick={() => setEditor(null)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveEditor}>{editor.kind === 'report' ? <><div className="field"><label htmlFor="edit-report-category">Tipo de reporte</label><input id="edit-report-category" value={editorForm.category || ''} onChange={(event) => setEditorForm({ ...editorForm, category: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-title">Título</label><input id="edit-report-title" value={editorForm.title || ''} onChange={(event) => setEditorForm({ ...editorForm, title: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-area">Estación o zona</label><input id="edit-report-area" value={editorForm.area || ''} onChange={(event) => setEditorForm({ ...editorForm, area: event.target.value })}/></div><div className="field"><label htmlFor="edit-report-description">Descripción corregida</label><textarea id="edit-report-description" rows="5" value={editorForm.description || ''} onChange={(event) => setEditorForm({ ...editorForm, description: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-status">Estado</label><select id="edit-report-status" value={editorForm.status || 'Pendiente de revisión'} onChange={(event) => setEditorForm({ ...editorForm, status: event.target.value })}><option>Pendiente de revisión</option><option>En revisión</option><option>Atendido</option><option>Requiere información</option></select></div></> : <><div className="form-grid"><div className="field"><label htmlFor="edit-ph">pH</label><input id="edit-ph" type="number" step="0.01" min="0" max="14" value={editorForm.ph} onChange={(event) => setEditorForm({ ...editorForm, ph: event.target.value })} required/></div><div className="field"><label htmlFor="edit-turbidity">Turbidez (NTU)</label><input id="edit-turbidity" type="number" step="0.1" min="0" value={editorForm.turbidity} onChange={(event) => setEditorForm({ ...editorForm, turbidity: event.target.value })} required/></div><div className="field"><label htmlFor="edit-temp">Temperatura (°C)</label><input id="edit-temp" type="number" step="0.1" min="0" value={editorForm.temperature} onChange={(event) => setEditorForm({ ...editorForm, temperature: event.target.value })} required/></div><div className="field"><label htmlFor="edit-flow">Caudal (m³/s)</label><input id="edit-flow" type="number" step="0.01" min="0" value={editorForm.flow} onChange={(event) => setEditorForm({ ...editorForm, flow: event.target.value })} required/></div></div><div className="field"><label htmlFor="edit-observation">Observación</label><textarea id="edit-observation" rows="3" value={editorForm.observation} onChange={(event) => setEditorForm({ ...editorForm, observation: event.target.value })}/></div></>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setEditor(null)}>Cancelar</button><button type="submit" className="button button-primary" disabled={editorBusy}>{editorBusy ? 'Guardando…' : <><Icon name="check" size={16}/> Guardar corrección</>}</button></div></form></section></div>}
  </div>;
}

export default App;
