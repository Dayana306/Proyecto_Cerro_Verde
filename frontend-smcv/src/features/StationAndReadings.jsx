import { useCallback, useEffect, useMemo, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { EmptyState, Pill, SectionTitle } from '../components/Shared.jsx';
import { apiRequest } from '../api.js';

const blankStation = {
  nombrePunto: '', descripcion: '', latitud: '', longitud: '', idZona: '', idArea: '',
};
const blankReading = {
  idPunto: '', fecha: localDate(), hora: localTime(), ph: '', turbidez: '',
  temperatura: '', caudal: '', observacion: '',
};

function localDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
function localTime() {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
}
function listFrom(result) {
  if (Array.isArray(result)) return result;
  return result?.content || result?.data?.content || result?.items || result?.data || [];
}
function findParameter(parameters, patterns) {
  return parameters.find((parameter) => {
    const code = String(parameter.codigo || '').trim().toUpperCase();
    const name = String(parameter.nombreParametro || parameter.nombre || '').toLowerCase().trim();
    return patterns.some((pattern) => {
      if (pattern === 'ph') return code === 'PH' || name === 'ph' || name.startsWith('ph ') || name.includes('potencial de hidrógeno');
      return code.includes(pattern.toUpperCase()) || name.includes(pattern);
    });
  });
}

export default function StationAndReadings({ isAdmin, points = [], records = [], recordsError = '', openAddReading = false, onAddReadingHandled, onRefresh, onEditReading, notify }) {
  const [search, setSearch] = useState('');
  const [zones, setZones] = useState([]);
  const [areas, setAreas] = useState([]);
  const [parameters, setParameters] = useState([]);
  const [stationModal, setStationModal] = useState(false);
  const [editingStation, setEditingStation] = useState(null);
  const [stationForm, setStationForm] = useState(blankStation);
  const [readingModal, setReadingModal] = useState(false);
  const [readingForm, setReadingForm] = useState(blankReading);
  const [busy, setBusy] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const loadCatalogs = useCallback(async () => {
    if (!isAdmin) return;
    const [zoneResult, areaResult, parameterResult] = await Promise.all([
      apiRequest('/api/zonas?estado=ACTIVO'),
      apiRequest('/api/areas?estado=ACTIVO'),
      apiRequest('/api/parametros?estado=ACTIVO'),
    ]);
    setZones(listFrom(zoneResult));
    setAreas(listFrom(areaResult));
    setParameters(listFrom(parameterResult));
  }, [isAdmin]);

  useEffect(() => {
    Promise.resolve().then(() => loadCatalogs()).catch((e) => setError(e.message || 'No se pudieron cargar los catálogos auxiliares.'));
  }, [loadCatalogs]);

  const filteredPoints = useMemo(() => points.filter((point) =>
    `${point.idPunto || ''} ${point.nombrePunto || ''} ${point.area || ''} ${point.zona || ''}`.toLowerCase().includes(search.toLowerCase())), [points, search]);

  const refreshEverything = async () => {
    setRefreshing(true);
    setError('');
    try {
      await onRefresh?.();
      if (isAdmin) await loadCatalogs();
      notify?.('Catálogo de estaciones e historial actualizados correctamente.');
    } catch (e) {
      setError(e.message || 'No se pudieron actualizar los datos.');
    } finally {
      setRefreshing(false);
    }
  };

  const openNewStation = () => {
    setError('');
    setEditingStation(null);
    setStationForm({ ...blankStation, idZona: zones[0]?.idZona ? String(zones[0].idZona) : '', idArea: areas[0]?.idArea ? String(areas[0].idArea) : '' });
    setStationModal(true);
  };
  const openEditStation = (point) => {
    setError('');
    setEditingStation(point);
    setStationForm({
      nombrePunto: point.nombrePunto || '', descripcion: point.descripcion || '',
      latitud: point.latitud ?? '', longitud: point.longitud ?? '',
      idZona: String(point.idZona ?? ''), idArea: String(point.idArea ?? ''),
    });
    setStationModal(true);
  };

  const saveStation = async (event) => {
    event.preventDefault();
    if (!isAdmin || busy) return;
    const latitud = Number(stationForm.latitud);
    const longitud = Number(stationForm.longitud);
    if (!Number.isFinite(latitud) || latitud < -90 || latitud > 90 || !Number.isFinite(longitud) || longitud < -180 || longitud > 180) {
      setError('Ingresa coordenadas válidas: latitud entre -90 y 90 y longitud entre -180 y 180.');
      return;
    }
    if (!stationForm.idZona || !stationForm.idArea) {
      setError('Selecciona una zona y un área activas para la estación.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const payload = {
        nombrePunto: stationForm.nombrePunto.trim(),
        descripcion: stationForm.descripcion.trim() || null,
        latitud, longitud, idZona: Number(stationForm.idZona), idArea: Number(stationForm.idArea),
      };
      if (editingStation) {
        await apiRequest(`/api/puntos-monitoreo/${editingStation.idPunto}`, { method: 'PUT', body: JSON.stringify(payload) });
        notify?.('Estación actualizada correctamente.');
      } else {
        await apiRequest('/api/puntos-monitoreo', { method: 'POST', body: JSON.stringify(payload) });
        notify?.('Estación agregada correctamente.');
      }
      setStationModal(false);
      await onRefresh?.();
    } catch (e) {
      setError(e.message || 'No se pudo guardar la estación.');
    } finally {
      setBusy(false);
    }
  };

  const toggleStation = async (point) => {
    if (!isAdmin) return;
    const next = point.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    if (!window.confirm(`¿Deseas ${next === 'ACTIVO' ? 'activar' : 'desactivar'} la estación ${point.nombrePunto}?`)) return;
    try {
      await apiRequest(`/api/puntos-monitoreo/${point.idPunto}/estado`, { method: 'PATCH', body: JSON.stringify({ estado: next }) });
      notify?.(`Estación ${next === 'ACTIVO' ? 'activada' : 'desactivada'}.`);
      await onRefresh?.();
    } catch (e) {
      setError(e.message || 'No se pudo cambiar el estado de la estación.');
    }
  };

  const openNewReading = () => {
    setError('');
    const firstActive = points.find((point) => point.estado === 'ACTIVO');
    setReadingForm({ ...blankReading, idPunto: firstActive?.idPunto ? String(firstActive.idPunto) : '', fecha: localDate(), hora: localTime() });
    setReadingModal(true);
  };

  useEffect(() => {
    if (!openAddReading || !isAdmin) return;
    setError('');
    const firstActive = points.find((point) => point.estado === 'ACTIVO');
    setReadingForm({ ...blankReading, idPunto: firstActive?.idPunto ? String(firstActive.idPunto) : '', fecha: localDate(), hora: localTime() });
    setReadingModal(true);
    onAddReadingHandled?.();
  }, [openAddReading, isAdmin, points, onAddReadingHandled]);

  const saveReading = async (event) => {
    event.preventDefault();
    if (!isAdmin || busy) return;
    const values = {
      ph: Number(readingForm.ph), turbidez: Number(readingForm.turbidez),
      temperatura: Number(readingForm.temperatura), caudal: Number(readingForm.caudal),
    };
    if (Object.values(values).some((value) => !Number.isFinite(value) || value < 0) || values.ph > 14) {
      setError('Verifica las mediciones: deben ser valores numéricos no negativos y el pH debe estar entre 0 y 14.');
      return;
    }
    const pH = findParameter(parameters, ['ph']);
    const turbidity = findParameter(parameters, ['turbidez', 'turb']);
    const temperature = findParameter(parameters, ['temperatura', 'temp']);
    const flow = findParameter(parameters, ['caudal', 'flujo']);
    if ([pH, turbidity, temperature, flow].some((parameter) => !parameter?.idParametro)) {
      setError('Faltan parámetros activos de pH, turbidez, temperatura o caudal. Contacta al administrador para revisar la configuración.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const saved = await apiRequest('/api/monitoreos', {
        method: 'POST',
        body: JSON.stringify({
          idPunto: Number(readingForm.idPunto), tipoMuestreo: 'MANUAL',
          horaMuestreo: `${readingForm.fecha}T${readingForm.hora}:00`,
          observacion: readingForm.observacion.trim() || null,
          lecturas: [
            { idParametro: pH.idParametro, valor: values.ph },
            { idParametro: turbidity.idParametro, valor: values.turbidez },
            { idParametro: temperature.idParametro, valor: values.temperatura },
            { idParametro: flow.idParametro, valor: values.caudal },
          ],
        }),
      });
      setReadingModal(false);
      try {
        await onRefresh?.();
        notify?.(`Lectura #${saved.idMonitoreo ?? ''} guardada correctamente.`);
      } catch {
        setError(`La lectura #${saved.idMonitoreo ?? ''} se guardó, pero no se pudo actualizar la tabla. Pulsa Actualizar para volver a cargar los datos.`);
        notify?.(`Lectura #${saved.idMonitoreo ?? ''} guardada. La tabla necesita actualizarse.`);
      }
    } catch (e) {
      setError(e.message || 'No se pudo registrar la lectura.');
    } finally {
      setBusy(false);
    }
  };

  return <div className="page-stack station-readings-page">
    <SectionTitle kicker="CONSULTA / 03" title="Estaciones de monitoreo" description={isAdmin ? 'Administra el catálogo de estaciones y registra o corrige lecturas de monitoreo.' : 'Consulta el catálogo de estaciones activas y el historial de lecturas disponibles.'} action={<button type="button" className="button button-secondary" onClick={refreshEverything} disabled={refreshing}><Icon name="refresh-cw" size={16}/>{refreshing ? 'Actualizando…' : 'Actualizar todo'}</button>}/>
    {error && <div className="auth-error" role="alert">{error}</div>}
    <section className="panel">
      <div className="panel-heading">
        <div><h2>Catálogo de estaciones</h2><p>{points.length} estaciones registradas.</p></div>
        <div className="published-alerts-actions">
          <label className="search-control"><Icon name="search" size={16}/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar estación…" aria-label="Buscar estación"/></label>
          {isAdmin && <button type="button" className="button button-primary button-small" onClick={openNewStation}><Icon name="plus" size={15}/> Agregar estación</button>}
        </div>
      </div>
      {filteredPoints.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Estación</th><th>Área</th><th>Zona</th><th>Coordenadas</th><th>Estado</th>{isAdmin && <th>Acciones</th>}</tr></thead><tbody>{filteredPoints.map((point) => <tr key={point.idPunto}><td><strong>{point.idPunto}</strong></td><td><strong>{point.nombrePunto}</strong><small className="table-subline">{point.descripcion || 'Sin descripción'}</small></td><td>{point.area || '—'}</td><td>{point.zona || '—'}</td><td>{point.latitud ?? '—'}, {point.longitud ?? '—'}</td><td><Pill tone={point.estado === 'ACTIVO' ? 'success' : 'neutral'}>{point.estado || 'SIN ESTADO'}</Pill></td>{isAdmin && <td><div className="table-actions"><button type="button" className="button button-secondary button-small" onClick={() => openEditStation(point)}><Icon name="edit" size={14}/> Editar</button><button type="button" className="button button-ghost button-small" onClick={() => toggleStation(point)}>{point.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button></div></td>}</tr>)}</tbody></table></div> : <EmptyState title="No hay estaciones para mostrar" detail="No hay estaciones para mostrar. Pulsa Actualizar todo o solicita al administrador que registre una estación."/>}
    </section>

    <section className="panel">
      <div className="panel-heading"><div><h2>Historial de lecturas</h2><p>{records.length} registros disponibles. Aquí puedes consultar las mediciones registradas.</p></div><div className="published-alerts-actions">{isAdmin && <button type="button" className="button button-primary button-small" onClick={openNewReading}><Icon name="plus" size={15}/> Agregar lectura</button>}<button type="button" className="button button-secondary button-small" onClick={refreshEverything} disabled={refreshing}><Icon name="refresh-cw" size={14}/> Actualizar</button></div></div>
      {records.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha y hora</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Temperatura</th><th>Caudal</th><th>Observación</th>{isAdmin && <th>Acciones</th>}</tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.temperature}</td><td>{record.flow}</td><td>{record.observation || '—'}</td>{isAdmin && <td><button type="button" className="button button-secondary button-small" onClick={() => onEditReading?.(record)}><Icon name="edit" size={14}/> Editar</button></td>}</tr>)}</tbody></table></div> : <EmptyState title={recordsError ? 'No se pudo consultar el historial' : 'Todavía no hay lecturas registradas'} detail={recordsError || (isAdmin ? 'Todavía no hay mediciones registradas. Usa Agregar lectura para ingresar la primera medición.' : 'Todavía no hay mediciones disponibles para mostrar.')}/>}
    </section>

    {stationModal && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setStationModal(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="station-edit-title"><div className="modal-heading"><div><span className="eyebrow">CATÁLOGO DE ESTACIONES</span><h2 id="station-edit-title">{editingStation ? 'Editar estación' : 'Agregar estación'}</h2><p>Registra nombre, ubicación geográfica, zona y área. Las coordenadas deben corresponder a la estación real.</p></div><button type="button" className="icon-button" onClick={() => setStationModal(false)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveStation}><div className="form-grid"><div className="field field-span-2"><label>Nombre de estación *</label><input maxLength="100" required value={stationForm.nombrePunto} onChange={(e) => setStationForm({ ...stationForm, nombrePunto: e.target.value })}/></div><div className="field field-span-2"><label>Descripción</label><input maxLength="200" value={stationForm.descripcion} onChange={(e) => setStationForm({ ...stationForm, descripcion: e.target.value })}/></div><div className="field"><label>Latitud *</label><input type="number" min="-90" max="90" step="0.000001" required value={stationForm.latitud} onChange={(e) => setStationForm({ ...stationForm, latitud: e.target.value })}/></div><div className="field"><label>Longitud *</label><input type="number" min="-180" max="180" step="0.000001" required value={stationForm.longitud} onChange={(e) => setStationForm({ ...stationForm, longitud: e.target.value })}/></div><div className="field"><label>Zona *</label><select required value={stationForm.idZona} onChange={(e) => setStationForm({ ...stationForm, idZona: e.target.value })}><option value="">Seleccionar zona</option>{zones.map((zone) => <option key={zone.idZona} value={zone.idZona}>{zone.nombreZona}</option>)}</select></div><div className="field"><label>Área *</label><select required value={stationForm.idArea} onChange={(e) => setStationForm({ ...stationForm, idArea: e.target.value })}><option value="">Seleccionar área</option>{areas.map((area) => <option key={area.idArea} value={area.idArea}>{area.nombreArea}</option>)}</select></div></div>{error && <div className="auth-error" role="alert">{error}</div>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setStationModal(false)} disabled={busy}>Cancelar</button><button type="submit" className="button button-primary" disabled={busy || !zones.length || !areas.length}>{busy ? 'Guardando…' : 'Guardar estación'} <Icon name="check" size={16}/></button></div></form></section></div>}

    {readingModal && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setReadingModal(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="reading-create-title"><div className="modal-heading"><div><span className="eyebrow">HISTORIAL DE LECTURAS</span><h2 id="reading-create-title">Agregar lectura</h2><p>Ingresa valores de una medición real. El servidor verificará los umbrales y guardará el registro.</p></div><button type="button" className="icon-button" onClick={() => setReadingModal(false)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveReading}><div className="form-grid"><div className="field field-span-2"><label>Estación *</label><select required value={readingForm.idPunto} onChange={(e) => setReadingForm({ ...readingForm, idPunto: e.target.value })}><option value="">Seleccionar estación</option>{points.filter((point) => point.estado === 'ACTIVO').map((point) => <option key={point.idPunto} value={point.idPunto}>{point.nombrePunto}</option>)}</select></div><div className="field"><label>Fecha *</label><input type="date" required max={localDate()} value={readingForm.fecha} onChange={(e) => setReadingForm({ ...readingForm, fecha: e.target.value })}/></div><div className="field"><label>Hora *</label><input type="time" required value={readingForm.hora} onChange={(e) => setReadingForm({ ...readingForm, hora: e.target.value })}/></div><div className="field"><label>pH *</label><input type="number" min="0" max="14" step="0.01" required value={readingForm.ph} onChange={(e) => setReadingForm({ ...readingForm, ph: e.target.value })}/></div><div className="field"><label>Turbidez (NTU) *</label><input type="number" min="0" step="0.001" required value={readingForm.turbidez} onChange={(e) => setReadingForm({ ...readingForm, turbidez: e.target.value })}/></div><div className="field"><label>Temperatura (°C) *</label><input type="number" min="0" step="0.001" required value={readingForm.temperatura} onChange={(e) => setReadingForm({ ...readingForm, temperatura: e.target.value })}/></div><div className="field"><label>Caudal (m³/s) *</label><input type="number" min="0" step="0.001" required value={readingForm.caudal} onChange={(e) => setReadingForm({ ...readingForm, caudal: e.target.value })}/></div><div className="field field-span-2"><label>Observación</label><textarea rows="3" maxLength="255" value={readingForm.observacion} onChange={(e) => setReadingForm({ ...readingForm, observacion: e.target.value })}/></div></div>{error && <div className="auth-error" role="alert">{error}</div>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setReadingModal(false)} disabled={busy}>Cancelar</button><button type="submit" className="button button-primary" disabled={busy || !points.some((point) => point.estado === 'ACTIVO')}>{busy ? 'Guardando…' : 'Guardar lectura'} <Icon name="check" size={16}/></button></div></form></section></div>}
  </div>;
}
