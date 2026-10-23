import { useState } from 'react';
import Icon from '../components/Icon.jsx';
import { DemoNotice, SectionTitle } from '../components/Shared.jsx';

export default function ReportSubmission({ onSubmit, notify }) {
  const [form, setForm] = useState({ category: 'Observación ambiental', title: '', area: '', description: '' });
  const update = (key, value) => setForm((current) => ({ ...current, [key]: value }));
  const submit = (event) => {
    event.preventDefault();
    if (!form.title.trim() || !form.description.trim()) { notify('Completa el título y la descripción del reporte.'); return; }
    onSubmit({ ...form, title: form.title.trim(), area: form.area.trim(), description: form.description.trim() });
    setForm({ category: 'Observación ambiental', title: '', area: '', description: '' });
  };
  return <div className="page-stack">
    <SectionTitle kicker="PARTICIPACIÓN / 06" title="Enviar un reporte" description="Comunica una observación para que pueda ser revisada por el administrador."/>
    <DemoNotice>Este formulario es visual y temporal: al enviar, el reporte se guarda únicamente en el estado de la página. No se transmite a una entidad ni genera una notificación fuera del navegador.</DemoNotice>
    <div className="report-submit-layout"><section className="panel form-panel"><div className="panel-heading"><div><h2>Datos del reporte</h2><p>Describe únicamente hechos que hayas observado o información que puedas sustentar.</p></div><span className="form-badge"><Icon name="shield" size={15}/> Revisión requerida</span></div>
      <form className="form-body" onSubmit={submit}>
        <div className="field"><label htmlFor="report-category">Tipo de reporte *</label><select id="report-category" value={form.category} onChange={(event) => update('category', event.target.value)}><option>Observación ambiental</option><option>Incidencia de monitoreo</option><option>Posible error en reporte</option><option>Solicitud de información</option><option>Otro</option></select></div>
        <div className="field"><label htmlFor="report-title">Título *</label><input id="report-title" maxLength={120} value={form.title} onChange={(event) => update('title', event.target.value)} placeholder="Resume el motivo del reporte" required/></div>
        <div className="field"><label htmlFor="report-area">Estación o zona (opcional)</label><input id="report-area" maxLength={120} value={form.area} onChange={(event) => update('area', event.target.value)} placeholder="Escribe el identificador si lo conoces"/></div>
        <div className="field"><label htmlFor="report-description">Descripción *</label><textarea id="report-description" rows="6" maxLength={4000} value={form.description} onChange={(event) => update('description', event.target.value)} placeholder="Incluye qué observaste, cuándo y qué información lo respalda. No agregues datos que no hayas verificado." required/></div>
        <div className="form-footer"><span>{form.description.length}/4000 caracteres</span><button className="button button-primary" type="submit"><Icon name="send" size={16}/> Enviar para revisión</button></div>
      </form>
    </section>
    <aside className="panel guidance-panel"><div className="guidance-icon"><Icon name="info" size={20}/></div><h3>¿Qué pasa después?</h3><div className="process-step"><span>1</span><div><strong>Envío</strong><p>El reporte aparece en “Reportes y auditoría” durante esta sesión.</p></div></div><div className="process-step"><span>2</span><div><strong>Revisión</strong><p>El administrador puede corregir el texto y actualizar el estado.</p></div></div><div className="process-step"><span>3</span><div><strong>Consulta y descarga</strong><p>El reporte puede incluirse en las exportaciones seleccionadas.</p></div></div><p className="modal-note">Estas etapas son simuladas en el frontend. Para recibir y gestionar reportes de forma real se necesitará una API, fuera del alcance visual actual.</p></aside></div>
  </div>;
}
