import { useEffect, useMemo, useRef, useState } from 'react';
import Icon from './components/Icon.jsx';
import { DemoNotice, EmptyState, MetricCard, Pill, SectionTitle } from './components/Shared.jsx';
import Map2D from './features/Map2D.jsx';
import ExportCenter from './features/ExportCenter.jsx';
import ReportSubmission from './features/ReportSubmission.jsx';
import { initialAlerts, initialAudit, initialRecords, navigation, stations } from './data/prototypeData.js';
import { apiRequest, clearSession, currentUser, login as apiLogin } from './api.js';

function todayDisplay() {
  return new Date().toLocaleDateString('es-PE');
}

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [role, setRole] = useState('Usuario normal');
  const [displayName, setDisplayName] = useState('');
  const [sessionUser, setSessionUser] = useState(null);
  const [entryScreen, setEntryScreen] = useState('welcome');
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');
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
  const [thresholds, setThresholds] = useState({ phMin: '6.0', phMax: '9.0', turbidityMax: '25', flowMax: '1.50' });
  const [showThresholds, setShowThresholds] = useState(false);
  const [editor, setEditor] = useState(null);
  const [editorForm, setEditorForm] = useState({});
  const roleLabel = (apiRole) => ({ ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', GERENTE: 'Gerente', AUDITOR: 'Auditor' }[apiRole] || 'Usuario');
  const roleForNavigation = (apiRole) => ({ ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', GERENTE: 'Usuario normal', AUDITOR: 'Usuario normal' }[apiRole] || 'Usuario normal');

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
        if (alertsResult.status === 'fulfilled') { const list = alertsResult.value?.content || alertsResult.value; if (Array.isArray(list)) setAlerts(list.map((a) => ({ id: String(a.idAlerta ?? a.id ?? ''), level: String(a.nivel || a.nivelAlerta || 'MEDIA').toLowerCase().includes('alta') ? 'Alta' : 'Media', title: a.descripcion || a.mensaje || a.nombreParametro || 'Alerta de monitoreo', station: a.nombrePunto || a.punto?.nombrePunto || 'Punto de monitoreo', value: a.valor != null ? String(a.valor) : '—', limit: a.limite != null ? String(a.limite) : '—', time: a.fechaHora || a.fechaGeneracion || '', status: a.estado || 'ABIERTA' }))); }
        if (recordsResult.status === 'fulfilled') { const list = recordsResult.value?.content || recordsResult.value; if (Array.isArray(list)) setRecords(list.map((r) => { const readings = r.lecturas || []; const valueFor = (name) => readings.find((l) => String(l.nombreParametro || l.parametro?.nombreParametro || '').toLowerCase().includes(name))?.valor ?? '—'; return { id: String(r.idMonitoreo ?? r.id ?? ''), station: r.nombrePunto || r.punto?.nombrePunto || 'Punto de monitoreo', date: r.horaMuestreo ? new Date(r.horaMuestreo).toLocaleDateString('es-PE') : '', time: r.horaMuestreo ? new Date(r.horaMuestreo).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '', ph: valueFor('ph'), turbidity: valueFor('turbidez'), temperature: valueFor('temperatura'), flow: valueFor('caudal'), observation: r.observacion || '' }; })); }
      } catch { clearSession(); }
    };
    restore();
    const expire = () => { setAuthenticated(false); setSessionUser(null); setEntryScreen('login'); setAuthError('Tu sesión ha vencido. Inicia sesión nuevamente.'); };
    window.addEventListener('smcv:session-expired', expire);
    return () => window.removeEventListener('smcv:session-expired', expire);
  }, []);

  const handleLogin = async (event) => {
    event.preventDefault();
    setAuthBusy(true); setAuthError('');
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
        if (Array.isArray(apiAlerts?.content || apiAlerts)) setAlerts((apiAlerts?.content || apiAlerts).map((a) => ({ id: String(a.idAlerta ?? a.id ?? ''), level: String(a.nivel || a.nivelAlerta || 'MEDIA').toLowerCase().includes('alta') ? 'Alta' : 'Media', title: a.descripcion || a.mensaje || a.nombreParametro || 'Alerta de monitoreo', station: a.nombrePunto || a.punto?.nombrePunto || 'Punto de monitoreo', value: a.valor != null ? String(a.valor) : '—', limit: a.limite != null ? String(a.limite) : '—', time: a.fechaHora || a.fechaGeneracion || '', status: a.estado || 'ABIERTA' })));
        if (Array.isArray(apiPoints)) setRemotePoints(apiPoints);
        if (Array.isArray(apiRecords?.content || apiRecords)) setRecords((apiRecords?.content || apiRecords).map((r) => { const readings = r.lecturas || []; const valueFor = (name) => readings.find((l) => String(l.nombreParametro || l.parametro?.nombreParametro || '').toLowerCase().includes(name))?.valor ?? '—'; return { id: String(r.idMonitoreo ?? r.id ?? ''), station: r.nombrePunto || r.punto?.nombrePunto || 'Punto de monitoreo', date: r.horaMuestreo ? new Date(r.horaMuestreo).toLocaleDateString('es-PE') : '', time: r.horaMuestreo ? new Date(r.horaMuestreo).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '', ph: valueFor('ph'), turbidity: valueFor('turbidez'), temperature: valueFor('temperatura'), flow: valueFor('caudal'), observation: r.observacion || '' }; }));
      } catch (loadError) { notify(`Sesión iniciada, pero algunos datos no se pudieron cargar: ${loadError.message}`); }
    } catch (error) { setAuthError(error.message || 'No se pudo iniciar sesión. Verifica tus credenciales y que el backend esté ejecutándose.'); }
    finally { setAuthBusy(false); }
  };

  const handleLogout = () => { clearSession(); setAuthenticated(false); setSessionUser(null); setView('dashboard'); setMobileNav(false); setEntryScreen('welcome'); };
  const [remotePoints, setRemotePoints] = useState(null);

  const isAdmin = role === 'Administrador';
  const allowedIds = sessionUser?.rol === 'AUDITOR' ? ['dashboard', 'estaciones', 'mapa2d', 'reportes'] : sessionUser?.rol === 'GERENTE' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'reportes'] : sessionUser?.rol === 'SUPERVISOR' ? ['dashboard', 'estaciones', 'mapa2d', 'alertas', 'reportes'] : null;
  const visibleNavigation = navigation.filter((item) => item.roles.includes(role) && (!allowedIds || allowedIds.includes(item.id)));
  const activeNavigation = visibleNavigation.find((item) => item.id === view) || visibleNavigation[0];

  const notify = (message) => {
    setToast(message);
    if (toastTimer.current) window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(''), 4200);
  };

  const navigate = (next) => {
    const allowed = navigation.find((item) => item.id === next)?.roles.includes(role);
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

  const saveEditor = (event) => {
    event.preventDefault();
    if (!isAdmin || !editor) return;
    if (editor.kind === 'report') {
      if (!editorForm.title.trim() || !editorForm.description.trim()) { notify('El título y la descripción son obligatorios.'); return; }
      const original = reports.find((item) => item.id === editor.id);
      setReports((current) => current.map((item) => item.id === editor.id ? { ...item, ...editorForm, title: editorForm.title.trim(), description: editorForm.description.trim() } : item));
      addAudit({ event: 'Reporte corregido', station: original?.area || 'No indicada', detail: `${editor.id}: texto o estado actualizado`, status: 'Registrado', kind: 'warning' });
      notify('Reporte corregido en la sesión actual.');
    } else {
      const numbers = ['ph', 'turbidity', 'temperature', 'flow'].map((key) => Number(editorForm[key]));
      if (numbers.some((value) => !Number.isFinite(value) || value < 0) || Number(editorForm.ph) > 14) { notify('Revisa los valores: deben ser números válidos y el pH no puede superar 14.'); return; }
      const original = records.find((item) => item.id === editor.id);
      setRecords((current) => current.map((item) => item.id === editor.id ? { ...item, ph: Number(editorForm.ph), turbidity: Number(editorForm.turbidity), temperature: Number(editorForm.temperature), flow: Number(editorForm.flow), observation: editorForm.observation.trim() } : item));
      addAudit({ event: 'Registro corregido', station: original?.station || '', detail: `${editor.id}: valores de monitoreo corregidos por el administrador`, status: 'Registrado', kind: 'warning' });
      notify('Registro corregido en la sesión actual.');
    }
    setEditor(null);
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

  const openThresholds = () => {
    if (!isAdmin) { notify('Solo el administrador puede modificar umbrales.'); return; }
    setShowThresholds(true);
  };

  const activeStations = remotePoints ? remotePoints.map((point) => ({ id: String(point.idPunto), name: point.nombrePunto, area: point.area || '—', ph: '—', turbidity: '—', flow: '—', temperature: '—', status: point.estado || 'Activo' })) : stations;
  const filteredStations = useMemo(() => activeStations.filter((station) => `${station.id} ${station.name} ${station.area}`.toLowerCase().includes(search.toLowerCase())), [search, remotePoints]);
  const filteredAlerts = useMemo(() => alerts.filter((item) => alertFilter === 'Todas' || (alertFilter === 'Abiertas' ? !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) : ['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()))), [alerts, alertFilter]);
  const pendingAlerts = alerts.filter((item) => !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length;
  const avgPh = remotePoints ? '—' : (stations.reduce((total, item) => total + item.ph, 0) / Math.max(stations.length, 1)).toFixed(2);
  const avgTurbidity = remotePoints ? '—' : (stations.reduce((total, item) => total + item.turbidity, 0) / Math.max(stations.length, 1)).toFixed(1);
  const avgFlow = remotePoints ? '—' : (stations.reduce((total, item) => total + item.flow, 0) / Math.max(stations.length, 1)).toFixed(2);

  if (!authenticated) return <main className="welcome-screen">
    <header className="welcome-header"><div className="brand"><span className="brand-mark"><Icon name="droplet" size={25}/></span><span><strong>CERRO VERDE</strong><small>SISTEMA DE MONITOREO HÍDRICO</small></span></div><span className="welcome-secure"><Icon name="shield" size={15}/> Acceso seguro</span></header>
    <div className="welcome-content"><section className="welcome-copy"><span className="welcome-kicker">PLATAFORMA DE INFORMACIÓN AMBIENTAL</span><h1>Datos claros para cuidar <em>cada recurso.</em></h1><p>Consulta el sistema de monitoreo, revisa información ambiental y gestiona las tareas autorizadas según tu cuenta.</p><div className="welcome-art" aria-hidden="true"><div className="art-glow"/><div className="server-rack"><span/><span/><span/><span/></div><div className="art-wave wave-one"/><div className="art-wave wave-two"/><div className="art-dot dot-one"/><div className="art-dot dot-two"/></div><div className="welcome-features"><span><Icon name="activity" size={16}/> Monitoreo</span><span><Icon name="map" size={16}/> Puntos y mapas</span><span><Icon name="bell" size={16}/> Alertas</span></div></section>
      <section className="welcome-panel">{entryScreen === 'welcome' ? <><span className="eyebrow">BIENVENIDO</span><h2>¿Cómo deseas continuar?</h2><p>Ingresa con una cuenta existente para acceder a las funciones disponibles para tu rol.</p><button className="welcome-action primary" onClick={() => { setEntryScreen('login'); setAuthError(''); }}><span className="welcome-action-icon"><Icon name="log-in" size={21}/></span><span><strong>Iniciar sesión</strong><small>Accede con tu correo y contraseña</small></span><Icon name="arrow-right" size={18}/></button><button className="welcome-action" onClick={() => setEntryScreen('register')}><span className="welcome-action-icon register-icon"><Icon name="user-plus" size={21}/></span><span><strong>Registrarse</strong><small>Información sobre la creación de cuentas</small></span><Icon name="arrow-right" size={18}/></button><div className="welcome-footnote"><Icon name="lock" size={16}/><span>La identidad y los permisos se verifican en el servidor.</span></div></> : entryScreen === 'login' ? <><button className="back-link" onClick={() => { setEntryScreen('welcome'); setAuthError(''); }}><Icon name="arrow-left" size={15}/> Volver</button><span className="eyebrow">ACCESO A LA PLATAFORMA</span><h2>Iniciar sesión</h2><p>Utiliza las credenciales de una cuenta registrada en el sistema.</p><form className="auth-form" onSubmit={handleLogin}><div className="field"><label htmlFor="login-email">Correo electrónico</label><input id="login-email" type="email" autoComplete="username" value={loginEmail} onChange={(e) => setLoginEmail(e.target.value)} placeholder="nombre@dominio.com" required/></div><div className="field"><label htmlFor="login-password">Contraseña</label><input id="login-password" type="password" autoComplete="current-password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} placeholder="Ingresa tu contraseña" required/></div>{authError && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{authError}</div>}<button className="button button-primary auth-submit" type="submit" disabled={authBusy}>{authBusy ? 'Verificando…' : 'Entrar al sistema'} <Icon name="arrow-right" size={17}/></button></form><div className="login-help"><Icon name="info" size={16}/><span>La API debe estar disponible en <code>{import.meta.env.VITE_API_URL || 'http://localhost:8080'}</code>. La cuenta de administrador de prueba documentada en el backend es <strong>admin@smcv.local</strong>.</span></div></> : <><button className="back-link" onClick={() => setEntryScreen('welcome')}><Icon name="arrow-left" size={15}/> Volver</button><span className="eyebrow">CREACIÓN DE CUENTAS</span><h2>Registrarse</h2><p>La API que acompaña este proyecto no documenta un registro público. Para no crear cuentas falsas ni modificar la base de datos desde el navegador, solicita al administrador que registre tu usuario.</p><div className="register-info"><Icon name="shield-check" size={22}/><div><strong>Registro administrado</strong><span>El backend documenta la creación de usuarios mediante una operación restringida al rol administrador.</span></div></div><button className="button button-primary auth-submit" onClick={() => setEntryScreen('login')}>Ir a iniciar sesión <Icon name="arrow-right" size={17}/></button></>}</section></div><footer className="welcome-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>Conexión mediante API segura</span></footer>
  </main>;

  return <div className={`app-shell ${mobileNav ? 'nav-open' : ''}`}>
    {mobileNav && <button className="mobile-scrim" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"/>}
    <aside className="sidebar"><div className="brand sidebar-brand"><span className="brand-mark"><Icon name="droplet" size={23}/></span><span><strong>CERRO VERDE</strong><small>MONITOREO HÍDRICO</small></span><button className="mobile-close" onClick={() => setMobileNav(false)} aria-label="Cerrar menú"><Icon name="close"/></button></div>
      <div className="sidebar-project"><span className="sidebar-project-dot"/><div><strong>Área de trabajo</strong><small>Panel ambiental</small></div></div><div className="nav-label">MENÚ PRINCIPAL</div>
      <nav className="sidebar-nav" aria-label="Navegación principal">{visibleNavigation.map((item) => <button key={item.id} className={`nav-link ${view === item.id ? 'nav-link-active' : ''}`} onClick={() => navigate(item.id)}><Icon name={item.icon} size={19}/><span>{item.label}</span>{item.id === 'alertas' && pendingAlerts > 0 && <small className="nav-count">{pendingAlerts}</small>}</button>)}</nav>
      <div className="sidebar-note"><Icon name="shield" size={18}/><strong>Permisos del perfil</strong><p>{isAdmin ? 'Gestión completa de la interfaz y corrección de reportes.' : 'Consulta, descarga y envío de reportes. Sin cambios administrativos.'}</p></div>
      <div className="sidebar-account"><div className="avatar">{isAdmin ? 'A' : 'U'}</div><div className="account-meta"><strong>{isAdmin ? 'Administrador' : 'Usuario normal'}</strong><small>{displayName}</small></div><button className="logout-icon" onClick={handleLogout} aria-label="Cerrar sesión" title="Cambiar perfil"><Icon name="logout" size={18}/></button></div>
    </aside>
    <main className="main-column"><header className="topbar"><div className="topbar-heading"><button className="mobile-menu" onClick={() => setMobileNav(true)} aria-label="Abrir menú"><Icon name="menu" size={22}/></button><div><span>Panel de control</span><strong>{activeNavigation?.label || 'Resumen general'}</strong></div></div><div className="topbar-right"><span className="demo-status"><i/>Conectado como {roleLabel(sessionUser?.rol)}</span><button className="topbar-alert" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell" size={19}/>{pendingAlerts > 0 && <i/>}</button><span className="topbar-separator"/><div className="topbar-profile"><span className="avatar avatar-small">{isAdmin ? 'A' : 'U'}</span><div><strong>{isAdmin ? 'Administrador' : 'Usuario normal'}</strong><small>Perfil de la sesión</small></div></div></div></header>
      <div className="content-area">
        {view === 'dashboard' && <div className="page-stack"><SectionTitle kicker="VISTA GENERAL / 01" title="Resumen del monitoreo" description="Resumen de monitoreo del sistema; los datos consultados se obtienen desde la API cuando está disponible." action={isAdmin ? <button className="button button-primary" onClick={() => navigate('monitoreo')}><Icon name="plus" size={16}/> Registrar medición</button> : <button className="button button-primary" onClick={() => navigate('enviar-reporte')}><Icon name="send" size={16}/> Enviar reporte</button>}/><div className="metrics-grid"><MetricCard label="Puntos de monitoreo" value={String(activeStations.length).padStart(2, '0')} unit="puntos" detail={remotePoints ? "Catálogo consultado en la API" : "Datos de vista previa; pendiente de conexión"} icon="pin" tone="teal"/><MetricCard label="pH medio referencial" value={avgPh} unit="pH" detail={remotePoints ? "Lecturas no incluidas en el catálogo de puntos" : "Calculado desde datos de vista previa"} icon="droplet" tone="blue"/><MetricCard label="Turbidez media" value={avgTurbidity} unit="NTU" detail={remotePoints ? "Lecturas no incluidas en el catálogo de puntos" : "Calculada desde datos de vista previa"} icon="activity" tone="green"/><MetricCard label="Caudal medio" value={avgFlow} unit="m³/s" detail={remotePoints ? "Lecturas no incluidas en el catálogo de puntos" : "Calculado desde datos de vista previa"} icon="activity" tone="amber"/><MetricCard label="Alertas no atendidas" value={String(pendingAlerts).padStart(2, '0')} unit="eventos" detail="Alertas cargadas desde la API cuando hay conexión" icon="bell" tone="red"/></div>
          <div className="dashboard-grid"><section className="panel"><div className="panel-heading"><div><h2>Lecturas recientes</h2><p>Registros disponibles en esta sesión</p></div><button className="text-link" onClick={() => navigate('estaciones')}>Ver estaciones <Icon name="logout" size={14}/></button></div>{records.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Caudal</th></tr></thead><tbody>{records.slice(0, 4).map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.flow}</td></tr>)}</tbody></table></div> : <EmptyState/>}</section>
          <section className="panel"><div className="panel-heading"><div><h2>Alertas recientes</h2><p>Eventos que requieren consulta</p></div><button className="icon-button" onClick={() => navigate('alertas')} aria-label="Abrir alertas"><Icon name="bell"/></button></div><div className="compact-alert-list">{alerts.slice(0, 4).map((item) => <article className="compact-alert" key={item.id}><span className={`alert-symbol alert-${item.level.toLowerCase()}`}><Icon name="alert" size={17}/></span><div><strong>{item.title}</strong><small>{item.station} · {item.time}</small></div><Pill tone={['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) ? 'success' : item.level === 'Alta' ? 'danger' : 'warning'}>{item.level}</Pill></article>)}</div></section></div>
        </div>}

        {view === 'monitoreo' && isAdmin && <div className="page-stack"><SectionTitle kicker="OPERACIÓN / 02" title="Registro de monitoreo" description="Registra mediciones en el backend. El servidor valida permisos, parámetros y umbrales antes de persistir los datos."/><section className="panel form-panel"><div className="panel-heading"><div><h2>Datos de la medición</h2><p>Ingresa los valores tomados de una fuente verificada.</p></div><Pill tone="info">Solo administrador</Pill></div><form className="form-body" onSubmit={submitMeasurement}><div className="form-grid"><div className="field field-span-2"><label htmlFor="measure-station">Estación *</label><select id="measure-station" name="station" required defaultValue={remotePoints?.[0]?.idPunto ? `${remotePoints[0].idPunto} · ${remotePoints[0].nombrePunto}` : `${stations[0].id} · ${stations[0].name}`}>{(remotePoints || stations).map((station) => <option key={station.idPunto ?? station.id} value={`${station.idPunto ?? station.id} · ${station.nombrePunto ?? station.name}`}>{station.idPunto ?? station.id} · {station.nombrePunto ?? station.name}</option>)}</select></div><div className="field"><label htmlFor="measure-date">Fecha *</label><input id="measure-date" name="date" type="date" required defaultValue={new Date().toISOString().slice(0, 10)}/></div><div className="field"><label htmlFor="measure-time">Hora *</label><input id="measure-time" name="time" type="time" required defaultValue="12:00"/></div><div className="field"><label htmlFor="measure-ph">pH *</label><input id="measure-ph" name="ph" type="number" step="0.01" min="0" max="14" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-turbidity">Turbidez (NTU) *</label><input id="measure-turbidity" name="turbidity" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-temperature">Temperatura (°C) *</label><input id="measure-temperature" name="temperature" type="number" step="0.1" min="0" required placeholder="Valor medido"/></div><div className="field"><label htmlFor="measure-flow">Caudal (m³/s) *</label><input id="measure-flow" name="flow" type="number" step="0.01" min="0" required placeholder="Valor medido"/></div><div className="field field-span-2"><label htmlFor="measure-observation">Observación</label><textarea id="measure-observation" name="observation" rows="3" placeholder="Describe solo observaciones verificadas"/></div></div><div className="form-footer"><span>La medición se enviará al backend y solo aparecerá como guardada si el servidor confirma la operación.</span><button className="button button-primary" type="submit"><Icon name="check" size={16}/> Validar y registrar</button></div></form></section></div>}

        {view === 'estaciones' && <div className="page-stack"><SectionTitle kicker="CONSULTA / 03" title="Estaciones de monitoreo" description="Consulta el catálogo visual disponible y las lecturas precargadas."/><DemoNotice/><section className="panel"><div className="panel-heading"><div><h2>Catálogo de estaciones</h2><p>El catálogo se consulta en la API; las lecturas requieren el historial de monitoreos correspondiente.</p></div><div className="search-control"><Icon name="search" size={16}/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar estación..." aria-label="Buscar estación"/></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Estación</th><th>Área</th><th>pH</th><th>Turbidez (NTU)</th><th>Caudal (m³/s)</th><th>Temperatura (°C)</th><th>Estado indicado</th></tr></thead><tbody>{filteredStations.map((station) => <tr key={station.id}><td><strong>{station.id}</strong></td><td>{station.name}</td><td>{station.area}</td><td>{station.ph}</td><td>{station.turbidity}</td><td>{station.flow}</td><td>{station.temperature}</td><td><Pill tone={station.status === 'Conforme' ? 'success' : 'warning'}>{station.status}</Pill></td></tr>)}</tbody></table></div></section><section className="panel"><div className="panel-heading"><div><h2>Historial de lecturas</h2><p>Registros heredados y añadidos durante esta sesión</p></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha y hora</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Temperatura</th><th>Caudal</th><th>Observación</th>{isAdmin && <th>Corrección</th>}</tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.temperature}</td><td>{record.flow}</td><td>{record.observation}</td>{isAdmin && <td><button className="button button-secondary button-small" onClick={() => openEditor('record', record)}><Icon name="edit" size={14}/> Corregir</button></td>}</tr>)}</tbody></table></div></section></div>}

        {view === 'mapa2d' && <Map2D notify={notify}/>}

        {view === 'alertas' && <div className="page-stack"><SectionTitle kicker="SEGUIMIENTO / 05" title="Centro de alertas" description={isAdmin ? 'Consulta y administra los estados de alerta visibles en el prototipo.' : 'Consulta las alertas disponibles; la atención administrativa está restringida.'} action={isAdmin && <button className="button button-secondary" onClick={openThresholds}><Icon name="shield" size={16}/> Configurar umbrales</button>}/><DemoNotice/><div className="alert-summary-grid"><article><span className="alert-tile-icon red"><Icon name="alert"/></span><strong>{alerts.filter((item) => item.level === 'Alta' && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Prioridad alta abierta</small></article><article><span className="alert-tile-icon amber"><Icon name="activity"/></span><strong>{alerts.filter((item) => item.level === 'Media' && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Prioridad media abierta</small></article><article><span className="alert-tile-icon green"><Icon name="check"/></span><strong>{alerts.filter((item) => ['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase())).length}</strong><small>Atendidas en sesión</small></article></div><section className="panel"><div className="panel-heading"><div><h2>Eventos visibles</h2><p>El seguimiento persistente requiere la API de alertas.</p></div><div className="filter-tabs">{['Todas', 'Abiertas', 'Atendidas'].map((filter) => <button key={filter} className={alertFilter === filter ? 'filter-active' : ''} onClick={() => setAlertFilter(filter)}>{filter}</button>)}</div></div>{filteredAlerts.length ? <div className="alerts-list">{filteredAlerts.map((item) => <article className="alert-row" key={item.id}><span className={`alert-symbol alert-${item.level.toLowerCase()}`}><Icon name="alert" size={19}/></span><div className="alert-main"><div className="alert-title-row"><strong>{item.title}</strong><Pill tone={item.level === 'Alta' ? 'danger' : 'warning'}>{item.level}</Pill></div><p>{item.station} · {item.time}</p><div className="alert-value-line"><span>Valor indicado <strong>{item.value}</strong></span><span>Referencia indicada <strong>{item.limit}</strong></span></div></div><div className="alert-side"><Pill tone={['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) ? 'success' : 'neutral'}>{item.status}</Pill>{isAdmin && !['ATENDIDA', 'CERRADA', 'CERRADO'].includes(String(item.status).toUpperCase()) && <button className="button button-secondary button-small" onClick={() => acknowledgeAlert(item.id)}><Icon name="check" size={14}/> Marcar atendida</button>}</div></article>)}</div> : <EmptyState title="No hay alertas en este filtro" detail="Prueba otra opción de filtro."/>}<p className="inline-note"><Icon name="info" size={15}/> Los valores precargados son de demostración; para alertas reales se requiere conexión con una fuente de datos validada.</p></section></div>}

        {view === 'enviar-reporte' && role === 'Usuario normal' && <ReportSubmission onSubmit={sendReport} notify={notify}/>}

        {view === 'reportes' && <ExportCenter role={role} records={records} stations={stations} alerts={alerts} audit={audit} reports={reports} onEditReport={(report) => openEditor('report', report)} onEditRecord={(record) => openEditor('record', record)} notify={notify}/>}

        <footer className="page-footer"><span>SMCV · Sistema de Monitoreo Hídrico</span><span>Frontend visual · información precargada pendiente de validación</span></footer>
      </div>
    </main>

    {toast && <div className="toast-message" role="status"><Icon name="check" size={17}/><span>{toast}</span><button onClick={() => setToast('')} aria-label="Cerrar aviso"><Icon name="close" size={15}/></button></div>}

    {showThresholds && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowThresholds(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="threshold-title"><div className="modal-heading"><div><span className="eyebrow">ADMINISTRACIÓN</span><h2 id="threshold-title">Umbrales de esta sesión</h2><p>Estos valores controlan únicamente las validaciones visuales del navegador.</p></div><button className="icon-button" onClick={() => setShowThresholds(false)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={(event) => { event.preventDefault(); if (Number(thresholds.phMin) >= Number(thresholds.phMax) || Object.values(thresholds).some((value) => !Number.isFinite(Number(value)))) { notify('Revisa los valores de los umbrales.'); return; } setShowThresholds(false); addAudit({ event: 'Umbrales actualizados', station: 'Global', detail: `pH ${thresholds.phMin}–${thresholds.phMax}; turbidez ${thresholds.turbidityMax}; caudal ${thresholds.flowMax}`, status: 'Registrado', kind: 'warning' }); notify('Umbrales actualizados para esta sesión.'); }}><div className="form-grid"><div className="field"><label htmlFor="th-ph-min">pH mínimo</label><input id="th-ph-min" type="number" step="0.1" value={thresholds.phMin} onChange={(event) => setThresholds({ ...thresholds, phMin: event.target.value })} required/></div><div className="field"><label htmlFor="th-ph-max">pH máximo</label><input id="th-ph-max" type="number" step="0.1" value={thresholds.phMax} onChange={(event) => setThresholds({ ...thresholds, phMax: event.target.value })} required/></div><div className="field"><label htmlFor="th-turb">Turbidez máxima (NTU)</label><input id="th-turb" type="number" min="0" step="0.1" value={thresholds.turbidityMax} onChange={(event) => setThresholds({ ...thresholds, turbidityMax: event.target.value })} required/></div><div className="field"><label htmlFor="th-flow">Caudal máximo (m³/s)</label><input id="th-flow" type="number" min="0" step="0.01" value={thresholds.flowMax} onChange={(event) => setThresholds({ ...thresholds, flowMax: event.target.value })} required/></div></div><div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setShowThresholds(false)}>Cancelar</button><button type="submit" className="button button-primary"><Icon name="check" size={16}/> Guardar umbrales</button></div></form></section></div>}

    {editor && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEditor(null); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="editor-title"><div className="modal-heading"><div><span className="eyebrow">CORRECCIÓN ADMINISTRATIVA</span><h2 id="editor-title">{editor.kind === 'report' ? `Corregir ${editor.id}` : `Corregir ${editor.id}`}</h2><p>La corrección se registra solo en la bitácora de esta sesión. No hay persistencia ni historial inmutable.</p></div><button className="icon-button" onClick={() => setEditor(null)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveEditor}>{editor.kind === 'report' ? <><div className="field"><label htmlFor="edit-report-category">Tipo de reporte</label><input id="edit-report-category" value={editorForm.category || ''} onChange={(event) => setEditorForm({ ...editorForm, category: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-title">Título</label><input id="edit-report-title" value={editorForm.title || ''} onChange={(event) => setEditorForm({ ...editorForm, title: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-area">Estación o zona</label><input id="edit-report-area" value={editorForm.area || ''} onChange={(event) => setEditorForm({ ...editorForm, area: event.target.value })}/></div><div className="field"><label htmlFor="edit-report-description">Descripción corregida</label><textarea id="edit-report-description" rows="5" value={editorForm.description || ''} onChange={(event) => setEditorForm({ ...editorForm, description: event.target.value })} required/></div><div className="field"><label htmlFor="edit-report-status">Estado</label><select id="edit-report-status" value={editorForm.status || 'Pendiente de revisión'} onChange={(event) => setEditorForm({ ...editorForm, status: event.target.value })}><option>Pendiente de revisión</option><option>En revisión</option><option>Atendido</option><option>Requiere información</option></select></div></> : <><div className="form-grid"><div className="field"><label htmlFor="edit-ph">pH</label><input id="edit-ph" type="number" step="0.01" min="0" max="14" value={editorForm.ph} onChange={(event) => setEditorForm({ ...editorForm, ph: event.target.value })} required/></div><div className="field"><label htmlFor="edit-turbidity">Turbidez (NTU)</label><input id="edit-turbidity" type="number" step="0.1" min="0" value={editorForm.turbidity} onChange={(event) => setEditorForm({ ...editorForm, turbidity: event.target.value })} required/></div><div className="field"><label htmlFor="edit-temp">Temperatura (°C)</label><input id="edit-temp" type="number" step="0.1" min="0" value={editorForm.temperature} onChange={(event) => setEditorForm({ ...editorForm, temperature: event.target.value })} required/></div><div className="field"><label htmlFor="edit-flow">Caudal (m³/s)</label><input id="edit-flow" type="number" step="0.01" min="0" value={editorForm.flow} onChange={(event) => setEditorForm({ ...editorForm, flow: event.target.value })} required/></div></div><div className="field"><label htmlFor="edit-observation">Observación</label><textarea id="edit-observation" rows="3" value={editorForm.observation} onChange={(event) => setEditorForm({ ...editorForm, observation: event.target.value })}/></div></>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setEditor(null)}>Cancelar</button><button type="submit" className="button button-primary"><Icon name="check" size={16}/> Guardar corrección</button></div></form></section></div>}
  </div>;
}

export default App;
