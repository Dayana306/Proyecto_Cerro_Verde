import { useState } from 'react';
import Icon from '../components/Icon.jsx';
import { DemoNotice, EmptyState, Pill, SectionTitle } from '../components/Shared.jsx';
import { exportCsv, exportPdf, exportPngTable, exportXlsx } from '../utils/exporters.js';

const EXPORT_OPTIONS = [
  { id: 'records', label: 'Registros de monitoreo', detail: 'Lecturas ya mostradas en el prototipo' },
  { id: 'stations', label: 'Estaciones', detail: 'Descripción y últimos valores de referencia' },
  { id: 'alerts', label: 'Alertas', detail: 'Eventos, niveles y estados visibles' },
  { id: 'audit', label: 'Auditoría', detail: 'Bitácora demostrativa y acciones de esta sesión' },
  { id: 'reports', label: 'Reportes enviados', detail: 'Reportes ingresados en la sesión actual' },
];

function toRows(selected, data) {
  const rows = [];
  if (selected.includes('records')) data.records.forEach((item) => rows.push({ Sección: 'Monitoreo', ID: item.id, Fecha: `${item.date} ${item.time}`, Estación: item.station, Detalle: `pH ${item.ph}; turbidez ${item.turbidity} NTU; temperatura ${item.temperature} °C; caudal ${item.flow} m³/s; observación: ${item.observation || ''}`, Estado: 'Dato del prototipo / sesión' }));
  if (selected.includes('stations')) data.stations.forEach((item) => rows.push({ Sección: 'Estación', ID: item.id, Fecha: '', Estación: item.name, Detalle: `Área: ${item.area}; pH ${item.ph}; turbidez ${item.turbidity} NTU; caudal ${item.flow} m³/s; temperatura ${item.temperature} °C`, Estado: item.status }));
  if (selected.includes('alerts')) data.alerts.forEach((item) => rows.push({ Sección: 'Alerta', ID: item.id, Fecha: item.time, Estación: item.station, Detalle: `${item.title}; valor: ${item.value}; referencia: ${item.limit}; nivel: ${item.level}`, Estado: item.status }));
  if (selected.includes('audit')) data.audit.forEach((item) => rows.push({ Sección: 'Auditoría', ID: item.id || '', Fecha: item.date, Estación: item.station, Detalle: `${item.event}; origen: ${item.origin}; ${item.detail}`, Estado: item.status }));
  if (selected.includes('reports')) data.reports.forEach((item) => rows.push({ Sección: 'Reporte enviado', ID: item.id, Fecha: item.date, Estación: item.area || 'No indicada', Detalle: `${item.category}: ${item.title}; ${item.description}`, Estado: item.status }));
  return rows;
}

export default function ExportCenter({ role, records, stations, alerts, audit, reports, onEditReport, onEditRecord, notify }) {
  const [selected, setSelected] = useState(['records', 'alerts', 'audit', 'reports']);
  const [auditSearch, setAuditSearch] = useState('');
  const isAdmin = role === 'Administrador';
  const availableOptions = isAdmin ? EXPORT_OPTIONS : EXPORT_OPTIONS.filter((option) => ['alerts', 'audit', 'reports'].includes(option.id));
  const allowedSelection = selected.filter((id) => availableOptions.some((option) => option.id === id));
  const exportRows = toRows(allowedSelection, { records, stations, alerts, audit, reports });
  const filteredAudit = audit.filter((item) => `${item.date} ${item.event} ${item.station} ${item.detail} ${item.origin}`.toLowerCase().includes(auditSearch.toLowerCase()));
  const toggle = (id) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const doExport = (format) => {
    if (!allowedSelection.length) { notify('Selecciona al menos una categoría de información.'); return; }
    if (!exportRows.length) { notify('Las categorías elegidas no contienen registros para exportar.'); return; }
    const stamp = new Date().toISOString().slice(0, 10);
    if (format === 'xlsx') exportXlsx(exportRows, `cerro-verde-${stamp}.xlsx`);
    if (format === 'csv') exportCsv(exportRows, `cerro-verde-${stamp}.csv`);
    if (format === 'png') exportPngTable(exportRows, `cerro-verde-${stamp}.png`);
    if (format === 'pdf') {
      const opened = exportPdf(exportRows, 'Reporte de monitoreo hídrico · Cerro Verde');
      if (!opened) notify('El navegador bloqueó la ventana de impresión. Permite ventanas emergentes para este sitio.');
    }
  };

  return <div className="page-stack">
    <SectionTitle kicker="DOCUMENTACIÓN / 07" title="Reportes y auditoría" description="Elige las secciones que necesitas y exporta únicamente la información seleccionada." action={<span className={`role-chip ${isAdmin ? 'role-admin' : 'role-user'}`}><Icon name={isAdmin ? 'shield' : 'user'} size={15}/>{isAdmin ? 'Acceso administrador' : 'Acceso usuario'}</span>}/>
    <DemoNotice>La exportación contiene los datos que están cargados en esta interfaz. Los valores precargados son demostrativos y deben contrastarse con el documento fuente antes de usarse como información oficial.</DemoNotice>

    <section className="panel export-panel">
      <div className="panel-heading"><div><h2>Seleccionar información</h2><p>{isAdmin ? 'Selecciona cualquier sección disponible para exportarla.' : 'Tu perfil permite descargar alertas, reportes enviados y auditoría.'}</p></div><span className="export-count">{exportRows.length} filas disponibles</span></div>
      <div className="export-options-grid">{availableOptions.map((option) => <label className={`export-option ${selected.includes(option.id) ? 'export-option-selected' : ''}`} key={option.id}><input type="checkbox" checked={selected.includes(option.id)} onChange={() => toggle(option.id)}/><span className="export-check"><Icon name="check" size={14}/></span><span className="export-option-text"><strong>{option.label}</strong><small>{option.detail}</small></span></label>)}</div>
      <div className="export-action-area"><div><span className="eyebrow">FORMATO DE DESCARGA</span><p>PDF · Excel (.xlsx) · PNG de resumen · CSV</p></div><div className="export-buttons"><button className="button button-secondary" onClick={() => doExport('pdf')}><Icon name="file" size={16}/> PDF</button><button className="button button-primary" onClick={() => doExport('xlsx')}><Icon name="download" size={16}/> Excel (.xlsx)</button><button className="button button-secondary" onClick={() => doExport('png')}><Icon name="download" size={16}/> PNG</button><button className="button button-quiet" onClick={() => doExport('csv')}>CSV</button></div></div>
      <p className="help-text">Para PDF, el navegador abrirá la ventana de impresión: elige “Guardar como PDF”. El Excel se genera como archivo .xlsx. El PNG muestra una tabla resumida de la selección.</p>
    </section>

    <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Reportes enviados</h2><p>{reports.length ? 'Reportes creados desde esta interfaz.' : 'Todavía no se han enviado reportes en esta sesión.'}</p></div><Pill tone={reports.length ? 'info' : 'neutral'}>{reports.length} reportes</Pill></div>
      {reports.length === 0 ? <EmptyState title="Aún no hay reportes enviados" detail="Los reportes aparecerán aquí cuando un usuario complete el formulario."/> : <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha</th><th>Tipo</th><th>Título y detalle</th><th>Estado</th>{isAdmin && <th>Acción administrativa</th>}</tr></thead><tbody>{reports.map((report) => <tr key={report.id}><td><strong>{report.id}</strong></td><td>{report.date}</td><td>{report.category}</td><td><strong>{report.title}</strong><div className="table-secondary">{report.description}</div><small>{report.area || 'Ubicación no indicada'}</small></td><td><Pill tone={report.status === 'Atendido' ? 'success' : report.status === 'En revisión' ? 'warning' : 'neutral'}>{report.status}</Pill></td>{isAdmin && <td><button className="button button-secondary button-small" onClick={() => onEditReport(report)}><Icon name="edit" size={14}/> Corregir reporte</button></td>}</tr>)}</tbody></table></div>}
    </section>

    <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Auditoría de la sesión</h2><p>Eventos heredados del prototipo y acciones ocurridas mientras esta página está abierta.</p></div><div className="search-control"><Icon name="search" size={16}/><input value={auditSearch} onChange={(event) => setAuditSearch(event.target.value)} placeholder="Buscar evento..." aria-label="Buscar en auditoría"/></div></div>
      {filteredAudit.length === 0 ? <EmptyState title="No hay coincidencias" detail="Prueba con otro término de búsqueda."/> : <div className="table-scroll"><table className="data-table"><thead><tr><th>Fecha y hora</th><th>Evento</th><th>Estación</th><th>Origen</th><th>Detalle</th><th>Estado</th></tr></thead><tbody>{filteredAudit.map((item, index) => <tr key={`${item.id || item.date}-${index}`}><td>{item.date}</td><td>{item.event}</td><td>{item.station}</td><td>{item.origin}</td><td>{item.detail}</td><td><Pill tone={item.kind === 'danger' ? 'danger' : item.kind === 'warning' ? 'warning' : 'neutral'}>{item.status}</Pill></td></tr>)}</tbody></table></div>}
    </section>

    {isAdmin && <section className="panel report-table-panel"><div className="panel-heading"><div><h2>Corrección de registros de monitoreo</h2><p>El administrador puede corregir errores de digitación. La vista añade el evento a la bitácora de sesión.</p></div></div>
      <div className="table-scroll"><table className="data-table"><thead><tr><th>ID</th><th>Fecha</th><th>Estación</th><th>pH</th><th>Turbidez</th><th>Caudal</th><th>Observación</th><th>Acción</th></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong>{record.id}</strong></td><td>{record.date} {record.time}</td><td>{record.station}</td><td>{record.ph}</td><td>{record.turbidity}</td><td>{record.flow}</td><td>{record.observation}</td><td><button className="button button-secondary button-small" onClick={() => onEditRecord(record)}><Icon name="edit" size={14}/> Corregir</button></td></tr>)}</tbody></table></div>
    </section>}
  </div>;
}
