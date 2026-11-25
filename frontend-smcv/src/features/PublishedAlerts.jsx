import { useCallback, useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { apiRequest } from '../api.js';

const blankForm = { titulo: '', descripcion: '', nivel: 'MEDIA', area: '' };
const priorityLabel = (nivel) => ({ ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' }[String(nivel || 'MEDIA').toUpperCase()] || 'Media');
const alertExamples = [
  { titulo: 'Mantenimiento preventivo de estación', descripcion: 'Se realizará una actividad de mantenimiento preventivo en la estación indicada. Durante la intervención, verifique la disponibilidad de los equipos y registre cualquier incidencia observada.' },
  { titulo: 'Inspección de infraestructura', descripcion: 'Se solicita realizar una inspección visual de la estación y confirmar el estado de la infraestructura, las condiciones de acceso y los equipos asociados.' },
  { titulo: 'Revisión de lecturas registradas', descripcion: 'Se requiere verificar las lecturas registradas, su fecha, unidad de medida y correspondencia con la estación seleccionada antes de validar el registro.' },
  { titulo: 'Aviso operativo para el personal', descripcion: 'Se comunica una actualización operativa para conocimiento del personal correspondiente. Revise las indicaciones aplicables y confirme la atención del aviso.' },
];
function fmtDate(value) {
  if (!value) return 'Fecha no indicada';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
}

export default function PublishedAlerts({ isAdmin = false, notify, onUnreadChange, onAlertsChange }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(blankForm);
  const [editingId, setEditingId] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await apiRequest('/api/alertas-publicadas');
      const list = Array.isArray(result) ? result : [];
      setItems(list);
      onUnreadChange?.(list.filter((item) => !item.leida).length);
      onAlertsChange?.(list);
    } catch (e) {
      setError(e.message || 'No se pudieron cargar las alertas publicadas.');
    } finally {
      setLoading(false);
    }
  }, [onUnreadChange, onAlertsChange]);

  useEffect(() => { load(); }, [load]);

  const saveAlert = async (event) => {
    event.preventDefault();
    if (!isAdmin || busy) return;
    if (!form.titulo.trim() || !form.descripcion.trim()) {
      setError('Completa el título y la descripción de la alerta.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await apiRequest(editingId ? `/api/alertas-publicadas/${editingId}` : '/api/alertas-publicadas', {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({
          titulo: form.titulo.trim(),
          descripcion: form.descripcion.trim(),
          nivel: form.nivel,
          area: form.area.trim() || null,
        }),
      });
      const wasEditing = Boolean(editingId);
      setForm(blankForm);
      setEditingId(null);
      setModalOpen(false);
      await load();
      notify?.(wasEditing ? 'Alerta actualizada correctamente.' : 'Alerta publicada correctamente.');
    } catch (e) {
      setError(e.message || 'No se pudo publicar la alerta.');
    } finally {
      setBusy(false);
    }
  };

  const markViewed = async (item) => {
    try {
      await apiRequest(`/api/alertas-publicadas/${item.idAlertaPublicada}/vista`, { method: 'PATCH' });
      await load();
    } catch (e) {
      setError(e.message || 'No se pudo actualizar el estado de lectura.');
    }
  };

  const unread = items.filter((item) => !item.leida).length;
  const openByPriority = (nivel) => items.filter((item) => String(item.nivel || 'MEDIA').toUpperCase() === nivel).length;

  return <section className="panel published-alerts-panel">
    <div className="panel-heading published-alerts-heading">
      <div>
        <span className="eyebrow">COMUNICADOS DEL SISTEMA</span>
        <h2>Alertas publicadas</h2>
        <p>Los avisos aparecen solo después de ser publicados por el administrador. Cada cuenta, incluida la administradora, mantiene su propio estado de lectura.</p>
      </div>
      <div className="published-alerts-actions">
        {unread > 0 && <span className="published-unread-count"><i /> {unread} disponibles sin ver</span>}
        <button type="button" className="button button-secondary button-small" onClick={load} disabled={loading}>
          <Icon name="refresh-cw" size={14} /> Actualizar
        </button>
        {isAdmin && <button type="button" className="button button-primary button-small" onClick={() => { setError(''); setEditingId(null); setForm(blankForm); setModalOpen(true); }}>
          <Icon name="plus" size={15} /> Agregar alerta
        </button>}
      </div>
    </div>

    {error && <div className="auth-error" role="alert">{error}</div>}
    <div className="published-priority-summary" aria-label="Cantidad de alertas publicadas por prioridad">
      <article className="published-priority-high"><strong>{openByPriority('ALTA')}</strong><span>Alta publicadas</span></article>
      <article className="published-priority-medium"><strong>{openByPriority('MEDIA')}</strong><span>Media publicadas</span></article>
      <article className="published-priority-low"><strong>{openByPriority('BAJA')}</strong><span>Baja publicadas</span></article>
    </div>
    {loading ? <div className="loading-inline"><Icon name="loader-circle" size={18} /> Cargando alertas publicadas…</div>
      : items.length ? <div className="published-alert-list">
        {items.map((item) => <article className={`published-alert-card ${item.leida ? 'published-alert-read' : 'published-alert-unread'}`} key={item.idAlertaPublicada}>
          <span className={`published-alert-icon published-alert-icon-${String(item.nivel || 'MEDIA').toLowerCase()}`}><Icon name="bell-ring" size={19} /></span>
          <div className="published-alert-content">
            <div className="published-alert-title"><strong>{item.titulo}</strong><span className={`published-level published-level-${String(item.nivel || 'MEDIA').toLowerCase()}`}>{priorityLabel(item.nivel)}</span></div>
            <p>{item.descripcion}</p>
            <small>{item.area ? `${item.area} · ` : ''}{fmtDate(item.fechaCreacion)} · Publicada por {item.autor || 'Administración'}</small>
          </div>
          <div className="published-alert-state">
            <>
              <span className={item.leida ? 'published-state-read' : 'published-state-unread'}>
                <i /> {item.leida ? 'Vista' : 'No vista'}
              </span>
              {!item.leida && <button type="button" className="button button-secondary button-small" onClick={() => markViewed(item)}>
                <Icon name="check" size={14} /> Marcar como vista
              </button>}
              {isAdmin && <button type="button" className="button button-secondary button-small" onClick={() => { setError(''); setEditingId(item.idAlertaPublicada); setForm({ titulo: item.titulo || '', descripcion: item.descripcion || '', nivel: String(item.nivel || 'MEDIA').toUpperCase(), area: item.area || '' }); setModalOpen(true); }}>
                <Icon name="edit" size={14} /> Editar alerta
              </button>}
            </>
          </div>
        </article>)}
      </div> : <div className="empty-state"><Icon name="bell" size={24} /><strong>No hay alertas publicadas</strong><span>{isAdmin ? 'Agrega un aviso para que los usuarios puedan consultarlo.' : 'Cuando administración publique un aviso, aparecerá aquí.'}</span></div>}

    {modalOpen && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) { setModalOpen(false); setEditingId(null); } }}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="published-alert-form-title">
        <div className="modal-heading"><div><span className="eyebrow">CENTRO DE ALERTAS</span><h2 id="published-alert-form-title">{editingId ? 'Editar alerta publicada' : 'Nueva alerta'}</h2><p>El aviso quedará visible para las cuentas activas y cada usuario podrá marcarlo como visto.</p></div>
          <button type="button" className="icon-button" onClick={() => { setModalOpen(false); setEditingId(null); }} aria-label="Cerrar"><Icon name="close" /></button>
        </div>
        <form className="form-body" onSubmit={saveAlert}>
          <div className="alert-example-block"><strong>Cuatro textos de ejemplo</strong><p>Selecciona uno para cargarlo en el formulario y edítalo. Los cambios solo se guardan al confirmar el botón inferior.</p><div className="alert-example-grid">{alertExamples.map((example) => <button key={example.titulo} type="button" className="button button-secondary button-small" onClick={() => setForm((current) => ({ ...current, ...example }))}>{example.titulo}</button>)}</div></div>
          <div className="field"><label htmlFor="published-alert-title">Título *</label><input id="published-alert-title" maxLength="120" value={form.titulo} onChange={(e) => setForm({ ...form, titulo: e.target.value })} required /></div>
          <div className="field"><label htmlFor="published-alert-description">Descripción *</label><textarea id="published-alert-description" rows="5" maxLength="2000" value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} required /></div>
          <div className="form-grid">
            <div className="field"><label htmlFor="published-alert-level">Prioridad</label><select id="published-alert-level" value={form.nivel} onChange={(e) => setForm({ ...form, nivel: e.target.value })}><option value="BAJA">Baja</option><option value="MEDIA">Media</option><option value="ALTA">Alta</option></select></div>
            <div className="field"><label htmlFor="published-alert-area">Área o sector</label><input id="published-alert-area" maxLength="120" value={form.area} onChange={(e) => setForm({ ...form, area: e.target.value })} placeholder="Opcional" /></div>
          </div>
          {error && <div className="auth-error" role="alert">{error}</div>}
          <div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => { setModalOpen(false); setEditingId(null); }} disabled={busy}>Cancelar</button><button type="submit" className="button button-primary" disabled={busy}>{busy ? (editingId ? 'Guardando…' : 'Publicando…') : (editingId ? 'Guardar cambios' : 'Publicar alerta')} <Icon name="send" size={15} /></button></div>
        </form>
      </section>
    </div>}
  </section>;
}
