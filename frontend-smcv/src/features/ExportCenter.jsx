import { useCallback, useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { EmptyState, Pill, SectionTitle } from '../components/Shared.jsx';
import { exportCsv, exportPdf, exportPngTable, exportPptx, exportXlsx } from '../utils/exporters.js';
import { apiRequest, downloadProtectedFile } from '../api.js';

const EXPORT_OPTIONS = [
  { id: 'records', label: 'Registros de monitoreo', detail: 'Lecturas ya mostradas en el prototipo' },
  { id: 'stations', label: 'Estaciones', detail: 'Descripción y últimos valores de referencia' },
  { id: 'alerts', label: 'Alertas', detail: 'Eventos, niveles y estados visibles' },
  { id: 'audit', label: 'Auditoría', detail: 'Registro de actividades disponible para perfiles autorizados.' },
  { id: 'reports', label: 'Reportes enviados', detail: 'Observaciones con sus adjuntos y documentos generados.' },
];

function toRows(selected, data) {
  const rows = [];
  if (selected.includes('records')) data.records.forEach((item) => rows.push({ Sección: 'Monitoreo', ID: item.id, Fecha: `${item.date} ${item.time}`, Estación: item.station, Detalle: `pH ${item.ph}; turbidez ${item.turbidity} NTU; temperatura ${item.temperature} °C; caudal ${item.flow} m³/s; observación: ${item.observation || ''}`, Estado: 'Registrado' }));
  if (selected.includes('stations')) data.stations.forEach((item) => rows.push({ Sección: 'Estación', ID: item.id, Fecha: '', Estación: item.name, Detalle: `Área: ${item.area}; pH ${item.ph}; turbidez ${item.turbidity} NTU; caudal ${item.flow} m³/s; temperatura ${item.temperature} °C`, Estado: item.status }));
  if (selected.includes('alerts')) data.alerts.forEach((item) => rows.push({ Sección: 'Alerta', ID: item.id, Fecha: item.time, Estación: item.station, Detalle: `${item.title}; valor: ${item.value}; referencia: ${item.limit}; nivel: ${item.level}`, Estado: item.status }));
  if (selected.includes('audit')) data.audit.forEach((item) => rows.push({ Sección: 'Auditoría', ID: item.id || '', Fecha: item.date, Estación: item.station, Detalle: `${item.event}; origen: ${item.origin}; ${item.detail}`, Estado: item.status }));
  if (selected.includes('reports')) data.reports.forEach((item) => rows.push({ Sección: 'Reporte enviado', ID: item.id, Fecha: item.date, Estación: item.area || 'No indicada', Detalle: `${item.category}: ${item.title}; ${item.description}`, Estado: item.status }));
  return rows;
}

function apiList(payload) {
  if (Array.isArray(payload)) return payload;
  const rows = payload?.content ?? payload?.data?.content ?? payload?.items ?? payload?.data ?? [];
  return Array.isArray(rows) ? rows : [];
}
async function fetchAllPages(path, pageSize = 100, sort = '') {
  const allRows = [];
  let page = 0;
  let done = false;
  while (!done && page < 1000) {
    const sortQuery = sort ? `&sort=${encodeURIComponent(sort)}` : '';
    const payload = await apiRequest(`${path}?page=${page}&size=${pageSize}${sortQuery}`);
    const rows = apiList(payload);
    if (!Array.isArray(payload) && !Array.isArray(payload?.content)
      && !Array.isArray(payload?.data?.content) && !Array.isArray(payload?.items)
      && !Array.isArray(payload?.data)) {
      throw new Error('No se pudo obtener la lista solicitada.');
    }
    allRows.push(...rows);
    const totalValue = payload?.totalPages ?? payload?.data?.totalPages;
    const totalPages = totalValue == null ? null : Number(totalValue);
    done = totalPages != null && Number.isInteger(totalPages) && totalPages >= 0
      ? totalPages === 0 || page + 1 >= totalPages
      : rows.length < pageSize;
    page += 1;
  }
  if (!done) throw new Error('No fue posible completar la consulta de registros.');
  return allRows;
}
function formatDate(value) {
  return value ? new Date(value).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
}
function localDateInput(value) {
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}
function initialReportRange() {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 6);
  return { fechaDesde: localDateInput(start), fechaHasta: localDateInput(end) };
}

export default function ExportCenter({ role, apiRole = '', records, recordsError = '', stations, alerts, audit, onEditRecord, onRefreshRecords, notify }) {
  const [selected, setSelected] = useState(['records', 'alerts', 'audit', 'reports']);
  const [auditSearch, setAuditSearch] = useState('');
  const [incidents, setIncidents] = useState([]);
  const [incidentFiles, setIncidentFiles] = useState({});
  const [generatedReports, setGeneratedReports] = useState([]);
  const [persistentAudit, setPersistentAudit] = useState([]);
  const [remoteLoading, setRemoteLoading] = useState(false);
  const [remoteError, setRemoteError] = useState('');
  const [auditLoadError, setAuditLoadError] = useState('');
  const [reportGenerating, setReportGenerating] = useState(false);
  const [refreshingAll, setRefreshingAll] = useState(false);
  const [refreshError, setRefreshError] = useState('');
  const [reportForm, setReportForm] = useState(() => ({
    tipoReporte: 'AMBIENTAL', formato: 'PDF', ...initialReportRange(), idPunto: '', descripcion: 'Reporte generado desde el módulo de reportes PTAR',
  }));
  const isAdmin = role === 'Administrador';
  const canReadAudit = ['ADMINISTRADOR', 'AUDITOR'].includes(String(apiRole).toUpperCase());
  const canGenerateReport = ['ADMINISTRADOR', 'GERENTE', 'AUDITOR'].includes(String(apiRole).toUpperCase());
  const loadPersistentData = useCallback(async () => {
    setRemoteLoading(true);
    setRemoteError('');
    const failures = [];
    const [incResult, reportResult, auditResult] = await Promise.allSettled([
      apiRequest('/api/operacion/incidencias'),
      fetchAllPages('/api/reportes', 100, 'fechaGeneracion,desc'),
      canReadAudit ? apiRequest('/api/auditoria/recientes?limite=5000') : Promise.resolve(null),
    ]);

    if (incResult.status === 'fulfilled') {
      const incList = apiList(incResult.value);
      setIncidents(incList);
      const filePairs = await Promise.all(incList.map(async (item) => {
        try {
          const files = await apiRequest(`/api/operacion/incidencias/${item.idIncidencia}/adjuntos`);
          return [item.idIncidencia, apiList(files)];
        } catch (error) {
          failures.push(`Adjuntos del reporte #${item.idIncidencia}: ${error.message || 'no se pudieron consultar'}`);
          return [item.idIncidencia, []];
        }
      }));
      setIncidentFiles(Object.fromEntries(filePairs));
    } else {
      failures.push(`Reportes recibidos: ${incResult.reason?.message || 'no se pudieron consultar'}`);
    }

    if (reportResult.status === 'fulfilled') {
      setGeneratedReports(reportResult.value);
    } else {
      failures.push(`Documentos generados: ${reportResult.reason?.message || 'no se pudieron consultar'}`);
    }

    if (canReadAudit) {
      if (auditResult.status === 'fulfilled') {
        const auditList = apiList(auditResult.value);
        if (!Array.isArray(auditList)) throw new Error('No se recibió la actividad solicitada.');
        const rows = auditList.map((item) => ({
          id: item.idHistorial,
          date: formatDate(item.fechaHora),
          event: item.accion || 'ACTIVIDAD',
          station: item.entidad ? `${item.entidad}${item.idEntidad != null ? ` #${item.idEntidad}` : ''}` : '—',
          origin: item.usuario || (item.idUsuario ? `Usuario #${item.idUsuario}` : 'Sistema'),
          detail: item.descripcion || 'Sin descripción',
          status: 'Registrado',
          kind: 'neutral',
        }));
        setPersistentAudit(rows);
        setAuditLoadError('');
      } else {
        const message = auditResult.reason?.message || 'no se pudo consultar la actividad';
        setAuditLoadError(message);
        failures.push(`Auditoría: ${message}`);
      }
    } else {
      setPersistentAudit([]);
      setAuditLoadError('');
    }

    setRemoteError(failures.join(' · '));
    setRemoteLoading(false);
    return failures.length === 0;
  }, [canReadAudit]);
  useEffect(() => { loadPersistentData(); }, [loadPersistentData]);

  const refreshAllData = async () => {
    setRefreshingAll(true); setRefreshError('');
    const tasks = [loadPersistentData()];
    if (typeof onRefreshRecords === 'function') tasks.push(onRefreshRecords());
    const results = await Promise.allSettled(tasks);
    const failed = results.find((result) => result.status === 'rejected' || result.value === false);
    if (failed) setRefreshError(failed.reason?.message || 'Una o más consultas no se pudieron actualizar. Vuelve a intentarlo. Si el problema continúa, contacta al administrador.');
    else { setRefreshError(''); notify?.('Lecturas, documentos y auditoría actualizados correctamente.'); }
    setRefreshingAll(false);
  };

  const generateStoredReport = async (event) => {
    event.preventDefault();
    if (!canGenerateReport || reportGenerating) return;
    if (!reportForm.fechaDesde || !reportForm.fechaHasta || reportForm.fechaDesde > reportForm.fechaHasta) {
      setRefreshError('Selecciona un periodo válido: la fecha inicial no puede ser posterior a la final.');
      return;
    }
    setReportGenerating(true); setRefreshError('');
    try {
      const payload = {
        tipoReporte: reportForm.tipoReporte,
        formato: reportForm.formato,
        fechaDesde: reportForm.fechaDesde,
        fechaHasta: reportForm.fechaHasta,
        idPunto: reportForm.idPunto ? Number(reportForm.idPunto) : null,
        descripcion: reportForm.descripcion.trim() || null,
      };
      const created = await apiRequest('/api/reportes', { method: 'POST', body: JSON.stringify(payload) });
      await loadPersistentData();
      try {
        await downloadProtectedFile(`/api/reportes/${created.idReporte}/descarga`, created.nombreArchivo || `reporte-${created.idReporte}`);
        notify?.(`Reporte #${created.idReporte} generado y descargado correctamente.`);
      } catch (downloadError) {
        notify?.(`El reporte #${created.idReporte} quedó guardado. No se pudo descargar automáticamente: ${downloadError.message || 'intenta desde la lista de documentos'}.`);
      }
    } catch (error) {
      setRefreshError(error.message || 'No se pudo generar y guardar el reporte.');
    } finally { setReportGenerating(false); }
  };

  const availableOptions = (isAdmin ? EXPORT_OPTIONS : EXPORT_OPTIONS.filter((option) => ['alerts', 'audit', 'reports'].includes(option.id))).filter((option) => option.id !== 'audit' || canReadAudit);
  const allowedSelection = selected.filter((id) => availableOptions.some((option) => option.id === id));
  const effectiveReports = incidents.map((item) => ({ id: `INC-${item.idIncidencia}`, date: formatDate(item.fechaReporte), category: item.categoria, title: item.titulo, area: item.zona, description: item.descripcion, status: item.estado, attachments: incidentFiles[item.idIncidencia] || [] }));
  const auditRows = canReadAudit ? persistentAudit : audit;
  const exportRows = toRows(allowedSelection, { records, stations, alerts, audit: auditRows, reports: effectiveReports });
  const filteredAudit = auditRows.filter((item) => `${item.date} ${item.event} ${item.station} ${item.detail} ${item.origin}`.toLowerCase().includes(auditSearch.toLowerCase()));
  const toggle = (id) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const doExport = (format) => {
    if (!allowedSelection.length) { notify('Selecciona al menos una categoría de información.'); return; }
    if (!exportRows.length) { notify('Las categorías elegidas no contienen registros para exportar.'); return; }
    const stamp = new Date().toISOString().slice(0, 10);
    if (format === 'xlsx') exportXlsx(exportRows, `cerro-verde-${stamp}.xlsx`);
    if (format === 'pptx') exportPptx(exportRows, 'Reporte de monitoreo hídrico · Cerro Verde', `cerro-verde-${stamp}.pptx`);
    if (format === 'csv') exportCsv(exportRows, `cerro-verde-${stamp}.csv`);
    if (format === 'png') exportPngTable(exportRows, `cerro-verde-${stamp}.png`);
    if (format === 'pdf') {
      const opened = exportPdf(exportRows, 'Reporte de monitoreo hídrico · Cerro Verde');
      if (!opened) notify('El navegador bloqueó la ventana de impresión. Permite ventanas emergentes para este sitio.');
    }
  };

  return <div className="page-stack">
    <SectionTitle kicker="DOCUMENTACIÓN / 07" title="Reportes y auditoría" description="Genera documentos y consulta los registros disponibles." action={<div className="published-alerts-actions"><span className={`role-chip ${isAdmin ? 'role-admin' : 'role-user'}`}><Icon name={isAdmin ? 'shield' : 'user'} size={15}/>{isAdmin ? 'Acceso administrador' : 'Acceso usuario'}</span><button className="button button-secondary button-small" onClick={refreshAllData} disabled={refreshingAll || remoteLoading}><Icon name="refresh-cw" size={14}/>{refreshingAll || remoteLoading ? 'Actualizando…' : 'Actualizar datos'}</button></div>}/>
    {(remoteError || refreshError) && <p className="inline-note" role="status">{[remoteError, refreshError].filter(Boolean).join(' · ')}</p>}

    <section className="panel export-panel">
      <div className="panel-heading"><div><h2>Seleccionar información</h2><p>{isAdmin ? 'Selecciona cualquier sección disponible para exportarla.' : 'Tu perfil permite descargar alertas, reportes enviados y auditoría.'}</p></div><span className="export-count">{exportRows.length} filas disponibles</span></div>
      <div className="export-options-grid">{availableOptions.map((option) => <label className={`export-option ${selected.includes(option.id) ? 'export-option-selected' : ''}`} key={option.id}><input type="checkbox" checked={selected.includes(option.id)} onChange={() => toggle(option.id)}/><span className="export-check"><Icon name="check" size={14}/></span><span className="export-option-text"><strong>{option.label}</strong><small>{option.detail}</small></span></label>)}</div>
      <div className="export-action-area"><div><span className="eyebrow">FORMATO DE DESCARGA</span><p>PDF · Excel (.xlsx) · PowerPoint (.pptx) · PNG · CSV</p></div><div className="export-buttons"><button className="button button-secondary" onClick={() => doExport('pdf')}><Icon name="file" size={16}/> PDF</button><button className="button button-primary" onClick={() => doExport('xlsx')}><Icon name="download" size={16}/> Excel (.xlsx)</button><button className="button button-secondary" onClick={() => doExport('png')}><Icon name="download" size={16}/> PNG</button><button className="button button-secondary" onClick={() => doExport('pptx')}><Icon name="file-text" size={16}/> PowerPoint</button><button className="button button-quiet" onClick={() => doExport('csv')}>CSV</button></div></div>
      <p className="help-text">Para PDF, elige “Guardar como PDF” en la ventana de impresión. Excel genera .xlsx y PowerPoint genera una presentación .pptx editable. Canva no es un formato independiente: descarga el PPTX y luego súbelo desde Canva en tu navegador para editarlo.</p><p className="help-text"><a className="button button-quiet button-small" href="https://www.canva.com/" target="_blank" rel="noreferrer">Abrir Canva</a></p>
    </section>

    {canGenerateReport && <section className="panel report-table-panel">
      <div className="panel-heading"><div><h2>Generar y guardar documento</h2><p>Esta acción genera el archivo, registra la información del reporte y permite volver a descargarlo.</p></div></div>
      <form className="form-body" onSubmit={generateStoredReport}>
        <div className="form-grid">
          <div className="field"><label htmlFor="stored-report-type">Tipo de reporte *</label><select id="stored-report-type" required value={reportForm.tipoReporte} onChange={(event) => setReportForm({ ...reportForm, tipoReporte: event.target.value })}><option value="AMBIENTAL">Ambiental</option><option value="AUDITORIA">Auditoría</option><option value="ESPECIAL">Especial</option></select></div>
          <div className="field"><label htmlFor="stored-report-format">Formato *</label><select id="stored-report-format" required value={reportForm.formato} onChange={(event) => setReportForm({ ...reportForm, formato: event.target.value })}><option value="PDF">PDF</option><option value="XLSX">Excel (.xlsx)</option><option value="PPTX">PowerPoint (.pptx)</option></select></div>
          <div className="field"><label htmlFor="stored-report-from">Fecha desde *</label><input id="stored-report-from" type="date" required value={reportForm.fechaDesde} onChange={(event) => setReportForm({ ...reportForm, fechaDesde: event.target.value })}/></div>
          <div className="field"><label htmlFor="stored-report-to">Fecha hasta *</label><input id="stored-report-to" type="date" required value={reportForm.fechaHasta} onChange={(event) => setReportForm({ ...reportForm, fechaHasta: event.target.value })}/></div>
          <div className="field"><label htmlFor="stored-report-station">Estación</label><select id="stored-report-station" value={reportForm.idPunto} onChange={(event) => setReportForm({ ...reportForm, idPunto: event.target.value })}><option value="">Todas las estaciones</option>{stations.map((station) => <option key={station.id} value={station.id}>{station.name}</option>)}</select></div>
          <div className="field"><label htmlFor="stored-report-description">Descripción</label><input id="stored-report-description" maxLength="255" value={reportForm.descripcion} onChange={(event) => setReportForm({ ...reportForm, descripcion: event.target.value })} placeholder="Descripción del documento"/></div>
        </div>
        <div className="modal-actions"><button className="button button-primary" type="submit" disabled={reportGenerating}>{reportGenerating ? 'Generando y guardando…' : 'Generar y guardar documento'} <Icon name="file-check" size={16}/></button></div>
      </form>
    </section>}

    <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Reportes enviados: observaciones e incidencias</h2><p>Aquí se muestran los reportes enviados por los usuarios y sus archivos de evidencia, incluidos los PDF. Los registros permanecen disponibles al volver a ingresar.</p></div><div className="published-alerts-actions"><Pill tone={incidents.length ? 'info' : 'neutral'}>{incidents.length} reportes</Pill><button className="button button-secondary button-small" onClick={loadPersistentData} disabled={remoteLoading}><Icon name="refresh-cw" size={14}/>{remoteLoading ? 'Actualizando…' : 'Actualizar'}</button></div></div>
      {incidents.length === 0 ? <EmptyState title={remoteLoading ? 'Consultando reportes…' : 'No hay reportes enviados'} detail="Cuando una cuenta envía una observación desde Centro de alertas, el registro y sus archivos adjuntos aparecen aquí."/> : <div className="operation-record-list">{incidents.map((item) => <article className="operation-record" key={item.idIncidencia}><div className="operation-record-icon"><Icon name="file-text" size={19}/></div><div className="operation-record-main"><div className="operation-record-title"><strong>{item.titulo}</strong><Pill tone={['ATENDIDA','CERRADA'].includes(String(item.estado).toUpperCase()) ? 'success' : 'warning'}>{String(item.estado || 'PENDIENTE').replaceAll('_',' ')}</Pill></div><p>#{item.idIncidencia} · {item.categoria} · {item.zona} · {formatDate(item.fechaReporte)}</p><div className="operation-record-description">{item.descripcion}</div><small>Enviado por {item.nombreReportante || `Usuario #${item.idUsuarioReporta}`} · Prioridad {item.prioridad || 'MEDIA'}</small>{(incidentFiles[item.idIncidencia] || []).length > 0 && <div className="selected-file-list">{incidentFiles[item.idIncidencia].map((file) => <div className="selected-file-item" key={file.idArchivo}><Icon name="paperclip" size={14}/><span>{file.nombreOriginal} · {file.tamanoBytes ? `${(file.tamanoBytes/1024/1024).toFixed(2)} MB` : ''}</span><button className="button button-secondary button-small" onClick={async () => { try { await downloadProtectedFile(`/api/operacion/archivos/${file.idArchivo}/descarga`, file.nombreOriginal || 'evidencia'); } catch (error) { notify(error.message || 'No se pudo descargar el adjunto.'); } }}>Descargar</button></div>)}</div>}</div></article>)}</div>}
    </section>

    <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Documentos generados y guardados</h2><p>Los datos de cada reporte quedan guardados y el archivo generado se conserva para volver a descargarlo. Las exportaciones locales del navegador no aparecen en esta lista.</p></div><Pill tone={generatedReports.length ? 'info' : 'neutral'}>{generatedReports.length} documentos</Pill></div>{generatedReports.length === 0 ? <EmptyState title={remoteLoading ? 'Consultando documentos…' : 'No hay documentos generados'} detail="Usa el formulario Generar y guardar documento para crear un PDF, Excel o PowerPoint persistente."/> : <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha de generación</th><th>Tipo</th><th>Formato</th><th>Periodo</th><th>Estación</th><th>Archivo</th><th>Estado</th><th>Descarga</th></tr></thead><tbody>{generatedReports.map((item) => <tr key={item.idReporte}><td><strong>{item.idReporte}</strong></td><td>{formatDate(item.fechaGeneracion)}</td><td>{item.tipoReporte}</td><td>{item.formato || '—'}</td><td>{item.fechaDesde} – {item.fechaHasta}</td><td>{item.punto || 'Todas las estaciones'}</td><td>{item.nombreArchivo || '—'}</td><td><Pill tone={item.estado === 'ANULADO' ? 'danger' : 'success'}>{item.estado}</Pill></td><td><button className="button button-secondary button-small" onClick={async () => { try { await downloadProtectedFile(`/api/reportes/${item.idReporte}/descarga`, item.nombreArchivo || `reporte-${item.idReporte}`); } catch (error) { notify(error.message || 'No se pudo descargar el documento.'); } }}><Icon name="download" size={14}/> Descargar</button></td></tr>)}</tbody></table></div>}</section>

    <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Auditoría de actividad</h2><p>Consulta el historial de actividad: inicio de sesión, mediciones creadas o corregidas, cambios de umbrales, alertas y reportes. Disponible para los perfiles Administrador y Auditor.</p></div><div className="published-alerts-actions"><div className="search-control"><Icon name="search" size={16}/><input value={auditSearch} onChange={(event) => setAuditSearch(event.target.value)} placeholder="Buscar evento..." aria-label="Buscar en auditoría"/></div><button className="button button-secondary button-small" onClick={loadPersistentData} disabled={remoteLoading}><Icon name="refresh-cw" size={14}/>Actualizar</button></div></div>
      {filteredAudit.length === 0 ? <EmptyState title={auditRows.length === 0 ? (auditLoadError ? 'No se pudo actualizar la actividad' : 'No hay actividad registrada') : 'No hay coincidencias'} detail={auditRows.length === 0 ? (auditLoadError ? 'Vuelve a actualizar esta sección. Si el problema continúa, solicita revisar el acceso a los registros.' : canReadAudit ? 'Todavía no hay operaciones disponibles para mostrar.' : 'El historial detallado está disponible para los perfiles Administrador y Auditor.') : 'Prueba con otro término de búsqueda.'}/> : <div className="table-scroll"><table className="data-table"><thead><tr><th>Fecha y hora</th><th>Evento</th><th>Estación</th><th>Origen</th><th>Detalle</th><th>Estado</th></tr></thead><tbody>{filteredAudit.map((item, index) => <tr key={`${item.id || item.date}-${index}`}><td>{item.date}</td><td>{item.event}</td><td>{item.station}</td><td>{item.origin}</td><td>{item.detail}</td><td><Pill tone={item.kind === 'danger' ? 'danger' : item.kind === 'warning' ? 'warning' : 'neutral'}>{item.status}</Pill></td></tr>)}</tbody></table></div>}
    </section>

    {isAdmin && <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Corrección de registros de monitoreo</h2><p>Permite corregir errores de digitación de una medición ya guardada. Al guardar, el sistema actualiza la medición, revisa nuevamente sus alertas y deja constancia del cambio en el historial de actividad.</p></div><button className="button button-secondary button-small" onClick={refreshAllData} disabled={refreshingAll || remoteLoading}><Icon name="refresh-cw" size={14}/>Actualizar lecturas</button></div>
      {records.length === 0 ? <EmptyState title={recordsError ? 'No se pudo consultar el historial' : 'No hay registros para corregir'} detail={recordsError || 'Cuando existan mediciones guardadas, aparecerán aquí con la opción para corregir sus valores.'}/> : <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Temperatura</th><th>Caudal</th><th>Observación</th><th>Acción</th></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.temperature}</td><td>{record.flow}</td><td>{record.observation || '—'}</td><td><button className="button button-secondary button-small" onClick={() => onEditRecord(record)}><Icon name="edit" size={14}/> Corregir</button></td></tr>)}</tbody></table></div>}
    </section>}
  </div>;
}
