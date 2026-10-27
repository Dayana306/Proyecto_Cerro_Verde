import Icon from './Icon.jsx';

export function SectionTitle({ kicker, title, description, action }) {
  return <div className="section-title"><div><span className="eyebrow">{kicker}</span><h1>{title}</h1>{description && <p>{description}</p>}</div>{action && <div className="section-title-action">{action}</div>}</div>;
}

export function Pill({ children, tone = 'neutral' }) {
  return <span className={`pill pill-${tone}`}><span className="pill-dot" />{children}</span>;
}

export function EmptyState({ title = 'No hay información para mostrar', detail = 'La información aparecerá aquí cuando se registre.' }) {
  return <div className="empty-state"><Icon name="file" size={28}/><strong>{title}</strong><span>{detail}</span></div>;
}

export function DemoNotice({ children = 'Modo visual de demostración: las cifras y los eventos precargados deben validarse antes de considerarse información oficial.' }) {
  return <div className="demo-notice"><Icon name="alert" size={17}/><span>{children}</span></div>;
}

export function MetricCard({ label, value, unit, detail, icon, tone = 'teal' }) {
  return <article className="metric-card"><span className={`metric-icon metric-${tone}`}><Icon name={icon} size={21}/></span><div className="metric-label">{label}</div><div className="metric-value">{value}<span>{unit}</span></div><p>{detail}</p></article>;
}
