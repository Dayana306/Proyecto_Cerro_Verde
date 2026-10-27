import { useCallback, useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { EmptyState, Pill } from '../components/Shared.jsx';
import { apiRequest } from '../api.js';

const LOCAL_MODIFICATION_KEY = 'smcv_equipment_modified_at';
const blankEquipment = {
  codigo: '', nombre: '', tipoEquipo: '', fabricante: '', modelo: '',
  modoLectura: 'MANUAL', estado: 'ACTIVO', estadoConexion: 'NO_APLICA',
  idPunto: '', idPersonalResponsable: '', ultimaRevision: '', proximaRevision: '', observaciones: '',
};

function getToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
function formatDate(value) {
  if (!value) return '—';
  const parsed = new Date(`${String(value).slice(0, 10)}T12:00:00`);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleDateString('es-PE', { dateStyle: 'medium' });
}
function formatDateTime(value) {
  if (!value) return 'Sin modificación registrada';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 'Sin modificación registrada' : parsed.toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' });
}
function readLocalModificationTimes() {
  try {
    const parsed = JSON.parse(localStorage.getItem(LOCAL_MODIFICATION_KEY) || '{}');
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}
function writeLocalModificationTime(id, timestamp) {
  try {
    const current = readLocalModificationTimes();
    current[String(id)] = timestamp;
    localStorage.setItem(LOCAL_MODIFICATION_KEY, JSON.stringify(current));
    return current;
  } catch {
    return readLocalModificationTimes();
  }
}
function reviewStatus(item) {
  if (!item.ultimaRevision) return { label: 'Pendiente de revisión', tone: 'warning' };
  if (item.proximaRevision && item.proximaRevision < getToday()) return { label: 'Revisión vencida', tone: 'danger' };
  return { label: 'Revisión registrada', tone: 'success' };
}

export default function MonitoringEquipmentRegister({ notify }) {
  const [equipment, setEquipment] = useState([]);
  const [people, setPeople] = useState([]);
  const [points, setPoints] = useState([]);
  const [localModifiedAt, setLocalModifiedAt] = useState(() => readLocalModificationTimes());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ ...blankEquipment });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    const [equipmentResult, peopleResult, pointsResult] = await Promise.allSettled([
      apiRequest('/api/operacion/equipos'),
      apiRequest('/api/operacion/personal'),
      apiRequest('/api/puntos-monitoreo?estado=ACTIVO'),
    ]);
    if (equipmentResult.status === 'fulfilled') {
      setEquipment(Array.isArray(equipmentResult.value) ? equipmentResult.value : []);
    } else {
      setError(`No se pudo consultar el registro de equipos: ${equipmentResult.reason?.message || 'error de API'}`);
    }
    if (peopleResult.status === 'fulfilled') setPeople(Array.isArray(peopleResult.value) ? peopleResult.value : []);
    if (pointsResult.status === 'fulfilled') setPoints(Array.isArray(pointsResult.value) ? pointsResult.value : []);
    setLocalModifiedAt(readLocalModificationTimes());
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  const startNew = () => {
    setEditing(null);
    setForm({ ...blankEquipment });
    setError('');
    setModalOpen(true);
  };

  const startEdit = (item) => {
    setEditing(item);
    setForm({
      codigo: item.codigo || '', nombre: item.nombre || '', tipoEquipo: item.tipoEquipo || '',
      fabricante: item.fabricante || '', modelo: item.modelo || '', modoLectura: item.modoLectura || 'MANUAL',
      estado: item.estado || 'ACTIVO', estadoConexion: item.estadoConexion || 'NO_APLICA',
      idPunto: item.idPunto == null ? '' : String(item.idPunto),
      idPersonalResponsable: item.idPersonalResponsable == null ? '' : String(item.idPersonalResponsable),
      ultimaRevision: item.ultimaRevision || '', proximaRevision: item.proximaRevision || '',
      observaciones: item.observaciones || '',
    });
    setError('');
    setModalOpen(true);
  };

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    const payload = {
      ...form,
      codigo: form.codigo.trim(),
      nombre: form.nombre.trim(),
      tipoEquipo: form.tipoEquipo.trim(),
      fabricante: form.fabricante.trim() || null,
      modelo: form.modelo.trim() || null,
      idPunto: form.idPunto ? Number(form.idPunto) : null,
      idPersonalResponsable: form.idPersonalResponsable ? Number(form.idPersonalResponsable) : null,
      ultimaRevision: form.ultimaRevision || null,
      proximaRevision: form.proximaRevision || null,
      observaciones: form.observaciones.trim() || null,
    };
    try {
      const saved = await apiRequest(editing?.idEquipo ? `/api/operacion/equipos/${editing.idEquipo}` : '/api/operacion/equipos', {
        method: editing?.idEquipo ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      });
      const id = saved?.idEquipo ?? editing?.idEquipo;
      if (id != null) setLocalModifiedAt(writeLocalModificationTime(id, new Date().toISOString()));
      setModalOpen(false);
      notify?.(editing ? 'Datos del equipo actualizados en MariaDB.' : 'Equipo agregado a MariaDB.');
      await load();
    } catch (saveError) {
      setError(saveError.message || 'No se pudieron guardar los datos del equipo.');
    } finally {
      setSaving(false);
    }
  };

  const recordReviewToday = async (item) => {
    const ok = window.confirm(`¿Registrar que hoy (${formatDate(getToday())}) se revisó el equipo ${item.codigo} · ${item.nombre}?`);
    if (!ok) return;
    setSaving(true);
    setError('');
    try {
      const payload = {
        codigo: item.codigo, nombre: item.nombre, tipoEquipo: item.tipoEquipo,
        fabricante: item.fabricante || null, modelo: item.modelo || null,
        modoLectura: item.modoLectura || 'MANUAL', estado: item.estado || 'ACTIVO',
        estadoConexion: item.estadoConexion || 'NO_APLICA',
        idPunto: item.idPunto ?? null, idPersonalResponsable: item.idPersonalResponsable ?? null,
        ultimaRevision: getToday(), proximaRevision: item.proximaRevision || null,
        observaciones: item.observaciones || null,
      };
      await apiRequest(`/api/operacion/equipos/${item.idEquipo}`, { method: 'PUT', body: JSON.stringify(payload) });
      setLocalModifiedAt(writeLocalModificationTime(item.idEquipo, new Date().toISOString()));
      notify?.(`Revisión de ${item.codigo} registrada en la base de datos.`);
      await load();
    } catch (reviewError) {
      setError(reviewError.message || 'No se pudo registrar la revisión del equipo.');
    } finally {
      setSaving(false);
    }
  };

  const nameByPersonId = (id) => {
    const person = people.find((item) => String(item.idPersonal) === String(id));
    return person ? `${person.nombres} ${person.apellidos}`.trim() : (id ? `Personal #${id}` : 'Sin responsable asignado');
  };
  const nameByPointId = (id) => points.find((item) => String(item.idPunto) === String(id))?.nombrePunto || (id ? `Punto #${id}` : 'Sin estación asociada');

  return <div className="page-stack monitoring-equipment-register">
    <section className="panel">
      <div className="panel-heading monitoring-equipment-heading">
        <div>
          <span className="eyebrow">REGISTRO DE MONITOREO / EQUIPOS</span>
          <h2>Máquinas, responsables y revisiones</h2>
          <p>Registro administrativo de los equipos utilizados, su responsable y el estado de sus revisiones.</p>
        </div>
        <div className="monitoring-equipment-actions">
          <button className="button button-secondary" type="button" onClick={load} disabled={loading || saving}><Icon name="refresh-cw" size={15}/> Actualizar</button>
          <button className="button button-primary" type="button" onClick={startNew}><Icon name="plus" size={16}/> Agregar equipo</button>
        </div>
      </div>
      <div className="inline-note monitoring-equipment-note"><Icon name="info" size={16}/><span>Solo el administrador puede ver y editar este registro. Las fechas de revisión se guardan en el backend. La hora de última modificación se conserva en este navegador porque la tabla actual de equipos no tiene un campo de modificación; no se altera la estructura de la base de datos.</span></div>
      {error && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{error}</div>}
      {loading ? <div className="loading-inline"><Icon name="loader-circle" size={18}/> Consultando equipos, responsables y estaciones…</div> : equipment.length === 0 ? <EmptyState title="Todavía no hay equipos registrados" detail="Agrega una máquina o instrumento para asignarle un responsable y controlar sus revisiones."/> : <div className="table-scroll"><table className="data-table monitoring-equipment-table"><thead><tr><th>Máquina / equipo</th><th>Estación</th><th>Encargado</th><th>Última revisión</th><th>Próxima revisión</th><th>Estado de revisión</th><th>Última modificación</th><th>Estado del equipo</th><th>Acciones</th></tr></thead><tbody>{equipment.map((item) => {
        const review = reviewStatus(item);
        const modifiedAt = localModifiedAt[String(item.idEquipo)];
        const tone = item.estado === 'ACTIVO' ? 'success' : item.estado === 'EN_MANTENIMIENTO' ? 'warning' : item.estado === 'DANADO' ? 'danger' : 'neutral';
        return <tr key={item.idEquipo}>
          <td><strong>{item.codigo} · {item.nombre}</strong><span className="table-subline">{item.tipoEquipo || 'Tipo sin indicar'}{item.modelo ? ` · ${item.modelo}` : ''}</span></td>
          <td>{nameByPointId(item.idPunto)}</td>
          <td>{nameByPersonId(item.idPersonalResponsable)}</td>
          <td>{formatDate(item.ultimaRevision)}</td>
          <td>{formatDate(item.proximaRevision)}</td>
          <td><Pill tone={review.tone}>{review.label}</Pill></td>
          <td>{formatDateTime(modifiedAt)}</td>
          <td><Pill tone={tone}>{String(item.estado || 'SIN ESTADO').replaceAll('_', ' ')}</Pill></td>
          <td><div className="table-actions monitoring-equipment-row-actions"><button className="button button-secondary button-small" type="button" onClick={() => startEdit(item)}><Icon name="edit" size={14}/> Editar</button><button className="button button-ghost button-small" type="button" onClick={() => recordReviewToday(item)} disabled={saving}><Icon name="check" size={14}/> Registrar revisión</button></div></td>
        </tr>;
      })}</tbody></table></div>}
    </section>

    {modalOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setModalOpen(false); }}><section className="modal-card monitoring-equipment-modal" role="dialog" aria-modal="true" aria-labelledby="monitoring-equipment-title">
      <div className="modal-heading"><div><span className="eyebrow">ADMINISTRACIÓN DE EQUIPOS</span><h2 id="monitoring-equipment-title">{editing ? `Editar ${editing.codigo}` : 'Agregar máquina o equipo'}</h2><p>Asigna la estación y a la persona responsable. Actualiza las fechas cuando se realice una revisión.</p></div><button className="icon-button" type="button" onClick={() => setModalOpen(false)} aria-label="Cerrar" disabled={saving}><Icon name="close"/></button></div>
      <form className="form-body" onSubmit={save}>
        <div className="form-grid">
          <div className="field"><label htmlFor="monitor-equipment-code">Código *</label><input id="monitor-equipment-code" required maxLength="35" value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} placeholder="Ej. EQ-001"/></div>
          <div className="field"><label htmlFor="monitor-equipment-name">Nombre de la máquina *</label><input id="monitor-equipment-name" required maxLength="130" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Nombre identificable del equipo"/></div>
          <div className="field"><label htmlFor="monitor-equipment-type">Tipo de equipo *</label><input id="monitor-equipment-type" required maxLength="80" value={form.tipoEquipo} onChange={(e) => setForm({ ...form, tipoEquipo: e.target.value })} placeholder="Ej. Sensor de turbidez"/></div>
          <div className="field"><label htmlFor="monitor-equipment-person">Persona encargada</label><select id="monitor-equipment-person" value={form.idPersonalResponsable} onChange={(e) => setForm({ ...form, idPersonalResponsable: e.target.value })}><option value="">Sin responsable asignado</option>{people.filter((person) => person.estado === 'ACTIVO').map((person) => <option key={person.idPersonal} value={person.idPersonal}>{person.nombres} {person.apellidos} · {person.cargo}</option>)}</select></div>
          <div className="field"><label htmlFor="monitor-equipment-point">Estación / lugar</label><select id="monitor-equipment-point" value={form.idPunto} onChange={(e) => setForm({ ...form, idPunto: e.target.value })}><option value="">Sin estación asociada</option>{points.map((point) => <option key={point.idPunto} value={point.idPunto}>{point.nombrePunto}</option>)}</select></div>
          <div className="field"><label htmlFor="monitor-equipment-status">Estado operativo *</label><select id="monitor-equipment-status" required value={form.estado} onChange={(e) => setForm({ ...form, estado: e.target.value })}><option value="ACTIVO">Activo</option><option value="EN_MANTENIMIENTO">En mantenimiento</option><option value="DANADO">Dañado</option><option value="INACTIVO">Inactivo</option></select></div>
          <div className="field"><label htmlFor="monitor-equipment-manufacturer">Fabricante</label><input id="monitor-equipment-manufacturer" maxLength="100" value={form.fabricante} onChange={(e) => setForm({ ...form, fabricante: e.target.value })}/></div>
          <div className="field"><label htmlFor="monitor-equipment-model">Modelo</label><input id="monitor-equipment-model" maxLength="100" value={form.modelo} onChange={(e) => setForm({ ...form, modelo: e.target.value })}/></div>
          <div className="field"><label htmlFor="monitor-equipment-reading-mode">Modo de lectura *</label><select id="monitor-equipment-reading-mode" required value={form.modoLectura} onChange={(e) => setForm({ ...form, modoLectura: e.target.value })}><option value="MANUAL">Manual</option><option value="AUTOMATICO">Automático</option><option value="SEMIAUTOMATICO">Semiautomático</option></select></div>
          <div className="field"><label htmlFor="monitor-equipment-connection">Conexión *</label><select id="monitor-equipment-connection" required value={form.estadoConexion} onChange={(e) => setForm({ ...form, estadoConexion: e.target.value })}><option value="NO_APLICA">No aplica</option><option value="CONECTADO">Conectado</option><option value="DESCONECTADO">Desconectado</option><option value="INTERMITENTE">Intermitente</option></select></div>
          <div className="field"><label htmlFor="monitor-equipment-last-review">Fecha de última revisión</label><input id="monitor-equipment-last-review" type="date" value={form.ultimaRevision} onChange={(e) => setForm({ ...form, ultimaRevision: e.target.value })}/></div>
          <div className="field"><label htmlFor="monitor-equipment-next-review">Fecha de próxima revisión</label><input id="monitor-equipment-next-review" type="date" min={form.ultimaRevision || undefined} value={form.proximaRevision} onChange={(e) => setForm({ ...form, proximaRevision: e.target.value })}/></div>
          <div className="field field-span-2"><label htmlFor="monitor-equipment-notes">Observaciones / detalle de revisión</label><textarea id="monitor-equipment-notes" rows="3" maxLength="1000" value={form.observaciones} onChange={(e) => setForm({ ...form, observaciones: e.target.value })} placeholder="Anota el resultado o detalle verificado de la revisión."/></div>
        </div>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <div className="modal-actions"><button className="button button-secondary" type="button" onClick={() => setModalOpen(false)} disabled={saving}>Cancelar</button><button className="button button-primary" type="submit" disabled={saving}><Icon name="check" size={15}/>{saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Agregar equipo'}</button></div>
      </form>
    </section></div>}
  </div>;
}
