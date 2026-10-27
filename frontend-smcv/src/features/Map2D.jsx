import { useEffect, useMemo, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { SectionTitle, Pill, EmptyState } from '../components/Shared.jsx';
import { downloadSvgAsPng } from '../utils/exporters.js';

const STORAGE_KEY = 'ptar-mapas-2d-v1';

// Mapas esquemáticos de ejemplo. Las posiciones X/Y solo distribuyen los puntos
// en el dibujo; no representan coordenadas geográficas oficiales.
const DEFAULT_MAPS = [
  {
    id: 'captacion', title: 'Mapa 1 · Captación y conducción', shortTitle: 'Captación y conducción', count: 3,
    subtitle: 'Vista inicial con tres puntos de referencia.', landscape: 'water',
    points: [
      { id: 'C-01', name: 'Punto de captación', area: 'Captación', descripcion: 'Punto de referencia para observar la toma de agua. Nombre y ubicación esquemáticos.', x: 132, y: 128 },
      { id: 'C-02', name: 'Canal de ingreso', area: 'Conducción', descripcion: 'Referencia ilustrativa del canal de ingreso; validar con información de campo.', x: 375, y: 270 },
      { id: 'C-03', name: 'Control de caudal', area: 'Monitoreo', descripcion: 'Punto de ejemplo para el control de caudal. No representa una coordenada oficial.', x: 612, y: 145 },
    ],
  },
  {
    id: 'tratamiento', title: 'Mapa 2 · Tratamiento y salida', shortTitle: 'Tratamiento y salida', count: 5,
    subtitle: 'Vista de tratamiento con cinco puntos de referencia.', landscape: 'plant',
    points: [
      { id: 'T-01', name: 'Entrada a PTAR', area: 'Ingreso', descripcion: 'Referencia esquemática del ingreso a la planta de tratamiento.', x: 105, y: 100 },
      { id: 'T-02', name: 'Pretratamiento', area: 'Tratamiento', descripcion: 'Punto ilustrativo para registrar una inspección de pretratamiento.', x: 282, y: 170 },
      { id: 'T-03', name: 'Cámara de proceso', area: 'Tratamiento', descripcion: 'Referencia esquemática de una cámara de proceso; confirmar ubicación real.', x: 470, y: 105 },
      { id: 'T-04', name: 'Control de salida', area: 'Salida', descripcion: 'Punto de ejemplo para el control del agua a la salida.', x: 585, y: 300 },
      { id: 'T-05', name: 'Punto de muestreo', area: 'Muestreo', descripcion: 'Referencia ilustrativa para registrar toma de muestras.', x: 240, y: 365 },
    ],
  },
  {
    id: 'monitoreo', title: 'Mapa 3 · Red ampliada de monitoreo', shortTitle: 'Red ampliada', count: 7,
    subtitle: 'Vista ampliada con siete puntos de referencia.', landscape: 'network',
    points: [
      { id: 'R-01', name: 'Punto norte', area: 'Red de monitoreo', descripcion: 'Punto de referencia ilustrativo en la zona norte del esquema.', x: 94, y: 100 },
      { id: 'R-02', name: 'Estación oeste', area: 'Red de monitoreo', descripcion: 'Estación de ejemplo para inspección y registro.', x: 245, y: 80 },
      { id: 'R-03', name: 'Punto central', area: 'Red de monitoreo', descripcion: 'Punto central del esquema; ubicación geográfica pendiente de validar.', x: 405, y: 160 },
      { id: 'R-04', name: 'Estación este', area: 'Red de monitoreo', descripcion: 'Estación de referencia del lado este del dibujo.', x: 630, y: 105 },
      { id: 'R-05', name: 'Punto suroeste', area: 'Red de monitoreo', descripcion: 'Punto ilustrativo para seguimiento en el sector suroeste.', x: 125, y: 315 },
      { id: 'R-06', name: 'Punto sur central', area: 'Red de monitoreo', descripcion: 'Punto ilustrativo de seguimiento en el sector sur.', x: 365, y: 365 },
      { id: 'R-07', name: 'Punto sureste', area: 'Red de monitoreo', descripcion: 'Punto de ejemplo en el sector sureste del esquema.', x: 625, y: 345 },
    ],
  },
];

function readSavedMaps() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_MAPS;
    const saved = JSON.parse(raw);
    if (!Array.isArray(saved) || saved.length !== DEFAULT_MAPS.length) return DEFAULT_MAPS;
    return DEFAULT_MAPS.map((base) => {
      const candidate = saved.find((item) => item.id === base.id);
      if (!candidate || !Array.isArray(candidate.points) || candidate.points.length !== base.count) return base;
      return {
        ...base,
        points: base.points.map((defaultPoint) => {
          const current = candidate.points.find((point) => point.id === defaultPoint.id);
          if (!current) return defaultPoint;
          const x = Number(current.x);
          const y = Number(current.y);
          return {
            ...defaultPoint,
            name: typeof current.name === 'string' ? current.name : defaultPoint.name,
            area: typeof current.area === 'string' ? current.area : defaultPoint.area,
            descripcion: typeof current.descripcion === 'string' ? current.descripcion : defaultPoint.descripcion,
            x: Number.isFinite(x) ? Math.max(60, Math.min(700, x)) : defaultPoint.x,
            y: Number.isFinite(y) ? Math.max(55, Math.min(405, y)) : defaultPoint.y,
          };
        }),
      };
    });
  } catch {
    return DEFAULT_MAPS;
  }
}

const blankEdit = { name: '', area: '', descripcion: '', x: '380', y: '235' };

function MapArtwork({ map, selectedPoint, onSelect }) {
  const paths = {
    water: <>
      <path d="M-20 66 C82 35 142 115 240 116 S369 75 450 130 S620 190 780 100" fill="none" stroke="#d6e8d7" strokeWidth="47" strokeLinecap="round" />
      <path d="M-20 66 C82 35 142 115 240 116 S369 75 450 130 S620 190 780 100" fill="none" stroke="#a6d3d4" strokeWidth="22" strokeLinecap="round" />
      <path d="M65 430 C170 320 230 355 315 285 S490 260 760 410" fill="none" stroke="#c8e1d0" strokeWidth="36" strokeLinecap="round" />
      <ellipse cx="535" cy="235" rx="72" ry="43" fill="#e1f0e6" stroke="#bed7c7" strokeWidth="2" />
    </>,
    plant: <>
      <path d="M40 92 H720 M40 210 H720 M40 328 H720 M150 44 V425 M365 44 V425 M580 44 V425" fill="none" stroke="#d4e5d9" strokeWidth="3" strokeDasharray="7 8" />
      <path d="M-10 265 C130 220 198 290 302 238 S500 190 780 245" fill="none" stroke="#c7e5e8" strokeWidth="47" strokeLinecap="round" />
      <path d="M-10 265 C130 220 198 290 302 238 S500 190 780 245" fill="none" stroke="#5bb1bf" strokeWidth="18" strokeLinecap="round" />
      <rect x="185" y="115" width="116" height="64" rx="10" fill="#e0eee4" stroke="#a8c6b1" strokeWidth="2" />
      <rect x="405" y="300" width="120" height="65" rx="10" fill="#e0eee4" stroke="#a8c6b1" strokeWidth="2" />
    </>,
    network: <>
      <path d="M80 90 C160 110 170 190 270 180 S360 160 420 225 S535 280 675 100" fill="none" stroke="#d0e5d4" strokeWidth="32" strokeLinecap="round" />
      <path d="M110 330 C190 270 260 330 340 360 S510 350 650 335" fill="none" stroke="#c4e0e3" strokeWidth="25" strokeLinecap="round" />
      <path d="M245 80 L245 210 L125 315 M405 160 L405 300 L365 365 M630 105 L630 260 L625 345 M245 210 L405 160 L630 260" fill="none" stroke="#8cc8b4" strokeWidth="3" strokeDasharray="8 7" />
      <circle cx="405" cy="245" r="90" fill="#e8f2e8" stroke="#c5ddca" strokeWidth="2" strokeDasharray="5 7" />
    </>,
  };

  return <svg id="cv-map-svg" className="map-svg map-svg-expanded map-artwork" viewBox="0 0 760 470" role="img" aria-label={`${map.shortTitle}, mapa esquemático con ${map.points.length} puntos`}>
    <defs>
      <pattern id="map-grid" width="32" height="32" patternUnits="userSpaceOnUse"><path d="M32 0H0V32" fill="none" stroke="#d8e5dd" strokeWidth=".8" /></pattern>
      <linearGradient id="map-land" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#f6faf5" /><stop offset="1" stopColor="#edf5ef" /></linearGradient>
    </defs>
    <rect width="760" height="470" rx="18" fill="url(#map-land)" />
    <rect width="760" height="470" rx="18" fill="url(#map-grid)" />
    {paths[map.landscape]}
    <g className="map-labels"><text x="26" y="28">{map.shortTitle.toUpperCase()}</text><text x="26" y="448">ESQUEMA ILUSTRATIVO · VALIDAR DATOS EN CAMPO</text></g>
    {map.points.map((point, index) => {
      const isSelected = selectedPoint === point.id;
      const labelLeft = point.x > 535;
      const labelY = Math.max(38, Math.min(410, point.y - (index % 2 === 0 ? 27 : -9)));
      const labelX = labelLeft ? point.x - 155 : point.x + 15;
      const labelWidth = 140;
      const labelText = `${point.id} · ${point.name}`.length > 22 ? `${point.id} · ${point.name.slice(0, 16)}…` : `${point.id} · ${point.name}`;
      return <g key={point.id} className={`map-point ${isSelected ? 'map-point-selected' : ''}`} role="button" tabIndex="0" aria-label={`Seleccionar ${point.id}, ${point.name}`} onClick={() => onSelect(point.id)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); onSelect(point.id); } }}>
        <circle className="map-point-halo" cx={point.x} cy={point.y} r={isSelected ? 25 : 19} />
        <circle className="map-point-circle" cx={point.x} cy={point.y} r="11" />
        <circle cx={point.x} cy={point.y} r="3.4" fill="#fff" />
        <rect className="map-point-label" x={Math.max(3, Math.min(617, labelX))} y={labelY} width={labelWidth} height="28" rx="7" />
        <text className="map-point-text" x={Math.max(12, Math.min(626, labelX + 8))} y={labelY + 18}>{labelText}</text>
      </g>;
    })}
    <g transform="translate(710 29)"><circle r="18" fill="#fff" stroke="#d6e2da" /><path d="M0 -11 5 5 0 1 -5 5Z" fill="#406b59" /><text y="29" textAnchor="middle" fontSize="10" fill="#66776d">N</text></g>
  </svg>;
}

export default function Map2D({ notify, isAdmin = false }) {
  const [maps, setMaps] = useState(readSavedMaps);
  const [selectedMapId, setSelectedMapId] = useState('captacion');
  const [selectedPointId, setSelectedPointId] = useState('C-02');
  const [pointModal, setPointModal] = useState(false);
  const [editForm, setEditForm] = useState(blankEdit);
  const [formError, setFormError] = useState('');

  const activeMap = maps.find((map) => map.id === selectedMapId) || maps[0];
  const activePoint = activeMap.points.find((point) => point.id === selectedPointId) || activeMap.points[0];
  const pointIndex = activeMap.points.findIndex((point) => point.id === activePoint?.id);
  const mapFilename = useMemo(() => `${activeMap.id}-mapa-2d-cerro-verde.png`, [activeMap.id]);

  useEffect(() => {
    try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(maps)); }
    catch { /* El visor funciona también si el navegador bloquea el almacenamiento local. */ }
  }, [maps]);

  const chooseMap = (mapId) => {
    const nextMap = maps.find((map) => map.id === mapId);
    if (!nextMap) return;
    setSelectedMapId(mapId);
    setSelectedPointId(nextMap.points[0]?.id || '');
  };

  const openPointEditor = () => {
    if (!isAdmin || !activePoint) return;
    setFormError('');
    setEditForm({
      name: activePoint.name,
      area: activePoint.area,
      descripcion: activePoint.descripcion,
      x: String(activePoint.x),
      y: String(activePoint.y),
    });
    setPointModal(true);
  };

  const savePoint = (event) => {
    event.preventDefault();
    if (!isAdmin || !activePoint) return;
    const name = editForm.name.trim();
    const area = editForm.area.trim();
    const x = Number(editForm.x);
    const y = Number(editForm.y);
    if (!name) { setFormError('El nombre del punto es obligatorio.'); return; }
    if (!Number.isFinite(x) || x < 60 || x > 700 || !Number.isFinite(y) || y < 55 || y > 405) {
      setFormError('La posición del dibujo debe estar entre X: 60–700 e Y: 55–405.'); return;
    }
    setMaps((current) => current.map((map) => map.id !== selectedMapId ? map : {
      ...map,
      points: map.points.map((point) => point.id !== activePoint.id ? point : {
        ...point, name: name.slice(0, 70), area: area.slice(0, 70), descripcion: editForm.descripcion.trim().slice(0, 400), x, y,
      }),
    }));
    setPointModal(false);
    notify?.('Punto actualizado en este navegador. Estos cambios no modifican MariaDB.');
  };

  const resetMaps = () => {
    if (!isAdmin) return;
    if (!window.confirm('¿Restablecer los tres mapas y sus puntos de ejemplo? Se perderán los cambios locales de este navegador.')) return;
    setMaps(DEFAULT_MAPS);
    setSelectedMapId('captacion');
    setSelectedPointId('C-02');
    notify?.('Los mapas volvieron a sus datos de referencia.');
  };

  return <div className="page-stack map2d-page">
    <SectionTitle
      kicker="VISUALIZACIÓN / 04"
      title="Mapas esquemáticos 2D"
      description="Elige uno de los tres mapas independientes para ver sus puntos, consultar sus datos y descargar su imagen."
      action={<div className="map-actions"><button className="button button-secondary" onClick={() => chooseMap(selectedMapId)}><Icon name="refresh-cw" size={16} /> Restablecer vista</button>{isAdmin && <button className="button button-secondary" onClick={resetMaps}><Icon name="refresh-cw" size={16} /> Restaurar mapas</button>}<button className="button button-primary" onClick={() => downloadSvgAsPng('cv-map-svg', mapFilename, notify)}><Icon name="download" size={17} /> Descargar imagen PNG</button></div>}
    />

    <section className="map-choice-grid" aria-label="Seleccionar uno de tres mapas">
      {maps.map((map, index) => {
        const isChosen = map.id === activeMap.id;
        return <button type="button" key={map.id} className={`map-choice-card ${isChosen ? 'map-choice-card-active' : ''}`} onClick={() => chooseMap(map.id)} aria-pressed={isChosen}>
          <span className={`map-choice-preview map-choice-preview-${map.landscape}`}><Icon name="map" size={24} /> <span className="map-choice-number">0{index + 1}</span></span>
          <span className="map-choice-copy"><strong>{map.shortTitle}</strong><small>{map.points.length} puntos de referencia</small><small>{map.subtitle}</small></span>
          <span className="map-choice-check">{isChosen ? <Icon name="check" size={16} /> : <Icon name="arrow-right" size={16} />}</span>
        </button>;
      })}
    </section>

    <div className="inline-note map-note"><Icon name="info" size={16} /><span>Los tres dibujos son esquemas ilustrativos con datos de ejemplo editables; las posiciones X/Y solo controlan la presentación del dibujo y no son coordenadas geográficas oficiales de Cerro Verde. Las ediciones del administrador se guardan en el navegador actual, no en MariaDB.</span></div>

    <div className="map-layout map-layout-expanded">
      <section className="panel map-panel">
        <div className="panel-heading map-current-heading"><div><span className="eyebrow">MAPA 0{maps.findIndex((map) => map.id === activeMap.id) + 1}</span><h2>{activeMap.title}</h2><p>{activeMap.points.length} puntos · {activeMap.subtitle}</p></div><Pill tone="warning">Esquemático</Pill></div>
        <div className="map-canvas-wrap map-canvas-expanded"><MapArtwork map={activeMap} selectedPoint={activePoint?.id} onSelect={setSelectedPointId} /></div>
        <div className="map-legend"><span><i className="legend-dot legend-station" /> Punto de referencia</span><span><i className="legend-river" /> Canal / trazo ilustrativo</span><span><i className="legend-zone" /> Área esquemática</span></div>
        <div className="map-point-list" aria-label="Puntos del mapa elegido">
          {activeMap.points.map((point) => <button type="button" key={point.id} className={`map-point-chip ${point.id === activePoint?.id ? 'map-point-chip-active' : ''}`} onClick={() => setSelectedPointId(point.id)} aria-pressed={point.id === activePoint?.id}><span>{point.id}</span><strong>{point.name}</strong></button>)}
        </div>
      </section>

      <aside className="panel map-details">
        <div className="panel-heading"><div><h2>Punto seleccionado</h2><p>Mapa: {activeMap.shortTitle}</p></div></div>
        {activePoint ? <>
          <div className="selected-station-icon"><Icon name="pin" size={25} /></div>
          <span className="eyebrow">PUNTO {String(pointIndex + 1).padStart(2, '0')} DE {activeMap.points.length}</span>
          <h3>{activePoint.id}</h3>
          <div className="detail-row"><span>Nombre</span><strong>{activePoint.name}</strong></div>
          <div className="detail-row"><span>Sector / área</span><strong>{activePoint.area || 'Sin indicar'}</strong></div>
          <div className="detail-row"><span>Posición en el dibujo</span><strong>X: {activePoint.x} · Y: {activePoint.y}</strong></div>
          <p className="map-data-note">{activePoint.descripcion || 'Sin descripción registrada.'}</p>
          {isAdmin && <button className="button button-secondary map-edit-button" onClick={openPointEditor}><Icon name="edit" size={15} /> Editar datos de este punto</button>}
        </> : <EmptyState title="Selecciona un punto" detail="Pulsa uno de los puntos del mapa para consultar sus datos." />}
      </aside>
    </div>

    {pointModal && isAdmin && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setPointModal(false); }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="map-point-edit-title">
      <div className="modal-heading"><div><span className="eyebrow">ADMINISTRACIÓN DE MAPAS 2D</span><h2 id="map-point-edit-title">Editar {activePoint?.id}</h2><p>Actualiza la etiqueta, el sector, la descripción y la posición visual del punto.</p></div><button type="button" className="icon-button" onClick={() => setPointModal(false)} aria-label="Cerrar"><Icon name="close" /></button></div>
      <form className="form-body" onSubmit={savePoint}>
        <div className="form-grid">
          <div className="field field-span-2"><label htmlFor="map-point-name">Nombre del punto *</label><input id="map-point-name" required maxLength="70" value={editForm.name} onChange={(event) => setEditForm({ ...editForm, name: event.target.value })} /></div>
          <div className="field field-span-2"><label htmlFor="map-point-area">Sector / área</label><input id="map-point-area" maxLength="70" value={editForm.area} onChange={(event) => setEditForm({ ...editForm, area: event.target.value })} /></div>
          <div className="field field-span-2"><label htmlFor="map-point-description">Descripción</label><textarea id="map-point-description" rows="3" maxLength="400" value={editForm.descripcion} onChange={(event) => setEditForm({ ...editForm, descripcion: event.target.value })} /></div>
          <div className="field"><label htmlFor="map-point-x">Posición X en el dibujo (60–700)</label><input id="map-point-x" required type="number" min="60" max="700" value={editForm.x} onChange={(event) => setEditForm({ ...editForm, x: event.target.value })} /></div>
          <div className="field"><label htmlFor="map-point-y">Posición Y en el dibujo (55–405)</label><input id="map-point-y" required type="number" min="55" max="405" value={editForm.y} onChange={(event) => setEditForm({ ...editForm, y: event.target.value })} /></div>
        </div>
        {formError && <div className="auth-error" role="alert">{formError}</div>}
        <div className="inline-note"><Icon name="info" size={15} /><span>Los cambios se guardan en el navegador actual. Para compartirlos entre computadoras y guardarlos en MariaDB se necesitaría una ampliación del backend/base de datos.</span></div>
        <div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setPointModal(false)}>Cancelar</button><button type="submit" className="button button-primary"><Icon name="check" size={16} /> Guardar cambios</button></div>
      </form>
    </section></div>}
  </div>;
}
