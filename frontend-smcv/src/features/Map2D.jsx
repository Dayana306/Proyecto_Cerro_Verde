import { useState } from 'react';
import Icon from '../components/Icon.jsx';
import { SectionTitle, Pill, DemoNotice } from '../components/Shared.jsx';
import { downloadSvgAsPng } from '../utils/exporters.js';

const points = [
  { id: 'P01', x: 118, y: 108, name: 'Bocatoma', area: 'Captación' },
  { id: 'P02', x: 295, y: 190, name: 'Río principal', area: 'Cauce principal' },
  { id: 'P03', x: 490, y: 278, name: 'Salida PTAR', area: 'Tratamiento' },
  { id: 'P04', x: 635, y: 118, name: 'Canal de derivación', area: 'Distribución' },
  { id: 'P05', x: 605, y: 365, name: 'Punto de reúso', area: 'Reúso' },
];

export default function Map2D({ notify }) {
  const [selected, setSelected] = useState('P02');
  const activePoint = points.find((point) => point.id === selected) || points[0];

  return <div className="page-stack">
    <SectionTitle kicker="VISUALIZACIÓN / 04" title="Mapa esquemático 2D" description="Vista conceptual de estaciones y tramos de monitoreo. Los elementos se pueden seleccionar." action={<button className="button button-primary" onClick={() => downloadSvgAsPng('cv-map-svg', 'mapa-2d-cerro-verde.png', notify)}><Icon name="download" size={17}/> Descargar PNG</button>}/>
    <DemoNotice>El mapa se dibuja con SVG y CSS propios. No contiene coordenadas geográficas ni confirma ubicaciones, impactos o relaciones reales; esas capas se incorporarán cuando se proporcione cartografía validada.</DemoNotice>
    <div className="map-layout">
      <section className="panel map-panel">
        <div className="panel-heading"><div><h2>Vista de la zona de monitoreo</h2><p>Esquema visual no georreferenciado</p></div><Pill tone="info">Esquemático</Pill></div>
        <div className="map-canvas-wrap">
          <svg id="cv-map-svg" className="map-svg" viewBox="0 0 760 470" role="img" aria-label="Mapa 2D esquemático de estaciones de monitoreo">
            <defs>
              <pattern id="map-grid" width="32" height="32" patternUnits="userSpaceOnUse"><path d="M 32 0 L 0 0 0 32" fill="none" stroke="#d8e5dd" strokeWidth=".8"/></pattern>
              <linearGradient id="river-gradient" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#6bbec7"/><stop offset="1" stopColor="#2798a9"/></linearGradient>
            </defs>
            <rect width="760" height="470" rx="18" fill="#f3f8f4"/>
            <rect width="760" height="470" rx="18" fill="url(#map-grid)"/>
            <path d="M-10 56 C95 26 139 103 220 112 S337 63 400 111 S520 178 603 155 S690 162 780 103" fill="none" stroke="#dae9d7" strokeWidth="42" strokeLinecap="round"/>
            <path d="M-10 56 C95 26 139 103 220 112 S337 63 400 111 S520 178 603 155 S690 162 780 103" fill="none" stroke="#c5dec6" strokeWidth="2" strokeDasharray="6 7"/>
            <path d="M70 -10 C125 52 170 88 192 132 S250 191 323 207 S390 268 460 286 S555 302 590 356 S666 410 780 430" fill="none" stroke="#cee7e7" strokeWidth="82" strokeLinecap="round"/>
            <path d="M70 -10 C125 52 170 88 192 132 S250 191 323 207 S390 268 460 286 S555 302 590 356 S666 410 780 430" fill="none" stroke="url(#river-gradient)" strokeWidth="27" strokeLinecap="round"/>
            <path d="M70 -10 C125 52 170 88 192 132 S250 191 323 207 S390 268 460 286 S555 302 590 356 S666 410 780 430" fill="none" stroke="#c9f3ef" strokeWidth="2" strokeDasharray="8 8"/>
            <path d="M60 320 C130 270 170 298 227 330 S335 382 398 359" fill="none" stroke="#d8e6d2" strokeWidth="28" strokeLinecap="round"/>
            <path d="M475 50 C535 70 575 64 646 48" fill="none" stroke="#d4e4d4" strokeWidth="24" strokeLinecap="round"/>
            <g className="map-labels"><text x="32" y="30">ZONA DE REFERENCIA</text><text x="28" y="440">Río / cauce ilustrativo</text></g>
            {points.map((point) => <g key={point.id} className={`map-point ${selected === point.id ? 'map-point-selected' : ''}`} role="button" tabIndex="0" aria-label={`Seleccionar ${point.id}, ${point.name}`} onClick={() => setSelected(point.id)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') setSelected(point.id); }}>
              <circle className="map-point-halo" cx={point.x} cy={point.y} r={selected === point.id ? 25 : 20}/><circle className="map-point-circle" cx={point.x} cy={point.y} r="11"/><circle cx={point.x} cy={point.y} r="3.4" fill="#fff"/>
              <rect className="map-point-label" x={point.id === 'P04' ? point.x - 180 : point.x + 15} y={point.y - 20} width={point.id === 'P04' ? 165 : 138} height="31" rx="7"/><text className="map-point-text" x={point.id === 'P04' ? point.x - 170 : point.x + 25} y={point.y + 1}>{point.id} · {point.name}</text>
            </g>)}
            <g transform="translate(700 28)"><circle r="19" fill="#fff" stroke="#d6e2da"/><path d="M0 -12 5 5 0 1 -5 5Z" fill="#406b59"/><text y="31" textAnchor="middle" fontSize="10" fill="#66776d">N</text></g>
          </svg>
        </div>
        <div className="map-legend"><span><i className="legend-dot legend-station"/> Estación de referencia</span><span><i className="legend-river"/> Cauce ilustrativo</span><span><i className="legend-zone"/> Zona esquemática</span></div>
      </section>
      <aside className="panel map-details"><div className="panel-heading"><div><h2>Elemento seleccionado</h2><p>Selecciona un punto en el mapa</p></div></div><div className="selected-station-icon"><Icon name="pin" size={25}/></div><span className="eyebrow">IDENTIFICADOR</span><h3>{activePoint.id}</h3><div className="detail-row"><span>Nombre</span><strong>{activePoint.name}</strong></div><div className="detail-row"><span>Área indicada</span><strong>{activePoint.area}</strong></div><div className="detail-row"><span>Coordenadas</span><strong>No cargadas</strong></div><div className="detail-row"><span>Ubicación real</span><strong>Pendiente de validar</strong></div><p className="map-data-note">Las posiciones se usan únicamente para la distribución gráfica. No deben utilizarse para decisiones operativas.</p></aside>
    </div>
  </div>;
}
