import { useCallback, useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { EmptyState, Pill, SectionTitle } from '../components/Shared.jsx';
import { apiRequest } from '../api.js';

function rowsOf(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

function formatDate(value) {
  if (!value) return 'Fecha no disponible';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('es-PE');
}

function toneForLevel(level) {
  const normalized = String(level || '').toUpperCase();
  if (normalized === 'ALTA' || normalized === 'CRITICA') return 'danger';
  if (normalized === 'MEDIA') return 'warning';
  return 'neutral';
}

export default function UserNotifications({ onUnreadChange, notify }) {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [markingAll, setMarkingAll] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    const [listResult, countResult] = await Promise.allSettled([
      apiRequest('/api/notificaciones?page=0&size=100&sort=fechaEnvio,desc'),
      apiRequest('/api/notificaciones/no-leidas/total'),
    ]);

    if (listResult.status === 'fulfilled') {
      setError('');
      const nextItems = rowsOf(listResult.value);
      setItems(nextItems);
      const calculatedUnread = nextItems.filter((item) => !item.leida).length;
      const nextUnread = countResult.status === 'fulfilled'
        ? Number(countResult.value?.total ?? calculatedUnread)
        : calculatedUnread;
      setUnread(nextUnread);
      onUnreadChange?.(nextUnread);
    } else {
      setError(listResult.reason?.message || 'No se pudieron consultar tus notificaciones.');
      if (countResult.status === 'fulfilled') {
        const nextUnread = Number(countResult.value?.total) || 0;
        setUnread(nextUnread);
        onUnreadChange?.(nextUnread);
      }
    }
    setLoading(false);
  }, [onUnreadChange]);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => active ? load() : undefined);
    return () => { active = false; };
  }, [load]);

  const markOneAsRead = async (item) => {
    if (item.leida || busyId !== null) return;
    setBusyId(item.idNotificacion);
    setError('');
    try {
      await apiRequest(`/api/notificaciones/${item.idNotificacion}/leida`, { method: 'PATCH' });
      setItems((current) => current.map((row) => row.idNotificacion === item.idNotificacion ? { ...row, leida: true, fechaLectura: new Date().toISOString() } : row));
      const nextUnread = Math.max(0, unread - 1);
      setUnread(nextUnread);
      onUnreadChange?.(nextUnread);
      notify?.('Notificación marcada como leída.');
    } catch (e) {
      setError(e.message || 'No se pudo marcar la notificación como leída.');
    } finally {
      setBusyId(null);
    }
  };

  const markAllAsRead = async () => {
    if (markingAll || unread === 0) return;
    setMarkingAll(true);
    setError('');
    try {
      await apiRequest('/api/notificaciones/leidas', { method: 'PATCH' });
      setItems((current) => current.map((item) => ({ ...item, leida: true, fechaLectura: item.fechaLectura || new Date().toISOString() })));
      setUnread(0);
      onUnreadChange?.(0);
      notify?.('Todas tus notificaciones fueron marcadas como leídas.');
    } catch (e) {
      setError(e.message || 'No se pudieron marcar todas las notificaciones.');
    } finally {
      setMarkingAll(false);
    }
  };

  return <div className="page-stack user-notifications-page">
    <SectionTitle
      kicker="COMUNICACIÓN / 07"
      title="Notificaciones"
      description="Consulta las notificaciones personales generadas por los eventos de monitoreo. Este apartado no publica ni administra alertas públicas."
      action={<button type="button" className="button button-secondary" onClick={() => { setLoading(true); setError(''); load(); }} disabled={loading}><Icon name="refresh-cw" size={15}/>{loading ? 'Actualizando…' : 'Actualizar'}</button>}
    />
    <section className="panel">
      <div className="panel-heading">
        <div><h2>Mis notificaciones</h2><p>{items.length} notificaciones · {unread} sin leer.</p></div>
        <button type="button" className="button button-secondary button-small" onClick={markAllAsRead} disabled={markingAll || loading || unread === 0}><Icon name="check-check" size={15}/>{markingAll ? 'Actualizando…' : 'Marcar todas como leídas'}</button>
      </div>
      {error && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{error}</div>}
      {loading ? <div className="loading-inline"><Icon name="loader-circle" size={17}/> Consultando tus notificaciones…</div>
        : items.length ? <div className="user-notification-list">
          {items.map((item) => <article key={item.idNotificacion} className={`user-notification-card ${item.leida ? 'user-notification-read' : 'user-notification-unread'}`}>
            <span className={`alert-symbol alert-${String(item.nivelAlerta || 'MEDIA').toLowerCase()}`}><Icon name="bell" size={17}/></span>
            <div className="user-notification-copy">
              <div className="user-notification-title"><strong>{item.mensaje}</strong><Pill tone={toneForLevel(item.nivelAlerta)}>{item.nivelAlerta || 'Aviso'}</Pill></div>
              <small>{formatDate(item.fechaEnvio)} · Canal {item.canal || 'PANEL'} · Alerta #{item.idAlerta ?? '—'}</small>
              {item.estadoAlerta && <small>Estado de la alerta: {item.estadoAlerta}</small>}
            </div>
            <div className="user-notification-actions">
              <Pill tone={item.leida ? 'success' : 'warning'}>{item.leida ? 'Leída' : 'Sin leer'}</Pill>
              {!item.leida && <button type="button" className="button button-secondary button-small" onClick={() => markOneAsRead(item)} disabled={busyId !== null}><Icon name="check" size={14}/>{busyId === item.idNotificacion ? 'Guardando…' : 'Marcar leída'}</button>}
            </div>
          </article>)}
        </div> : <EmptyState title="No tienes notificaciones" detail="Cuando una medición genere una alerta según los umbrales configurados y el sistema te notifique, aparecerá aquí."/>}
    </section>
  </div>;
}
