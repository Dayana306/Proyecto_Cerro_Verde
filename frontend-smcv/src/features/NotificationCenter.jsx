import { useCallback, useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { EmptyState, Pill, SectionTitle } from '../components/Shared.jsx';
import { apiRequest } from '../api.js';

function rowsOf(payload) {
  if (Array.isArray(payload)) return payload;
  const rows = payload?.content ?? payload?.data?.content ?? payload?.items ?? [];
  return Array.isArray(rows) ? rows : [];
}

function when(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('es-PE');
}

export default function NotificationCenter({ notify }) {
  const [mailStatus, setMailStatus] = useState(null);
  const [history, setHistory] = useState([]);
  const [recipients, setRecipients] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState([]);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    setLoading(true); setError('');
    const [statusResult, historyResult] = await Promise.allSettled([
      apiRequest('/api/notificaciones/correos/configuracion'),
      apiRequest('/api/notificaciones/correos?page=0&size=30&sort=fechaEnvio,desc'),
    ]);
    if (statusResult.status === 'fulfilled') setMailStatus(statusResult.value);
    else setMailStatus(null);
    if (historyResult.status === 'fulfilled') setHistory(rowsOf(historyResult.value));
    else { setHistory([]); setError(historyResult.reason?.message || 'No se pudo consultar el historial de correos.'); }
    setLoading(false);
  }, []);

  useEffect(() => { Promise.resolve().then(refresh); }, [refresh]);

  const send = async (event) => {
    event.preventDefault();
    if (busy) return;
    setError('');
    const addressList = recipients.split(/[;,\s]+/).filter(Boolean);
    if (!addressList.length) { setError('Escribe al menos un correo destinatario.'); return; }
    if (addressList.length > 30) { setError('Puedes enviar como máximo a 30 destinatarios por mensaje.'); return; }
    if (!subject.trim() || !message.trim()) { setError('Completa el asunto y el mensaje.'); return; }
    if (files.length > 5) { setError('Puedes adjuntar como máximo 5 archivos.'); return; }
    if (files.some((file) => file.size > 10 * 1024 * 1024)) { setError('Cada archivo adjunto debe pesar como máximo 10 MB.'); return; }
    const form = new FormData();
    form.append('destinatarios', recipients);
    form.append('asunto', subject.trim());
    form.append('mensaje', message);
    form.append('html', 'false');
    files.forEach((file) => form.append('adjuntos', file));
    setBusy(true);
    try {
      const result = await apiRequest('/api/notificaciones/correos', { method: 'POST', body: form });
      if (result?.estado === 'ENVIADO') {
        notify?.(`Notificación enviada correctamente. Registro #${result.idCorreo}.`);
        setRecipients(''); setSubject(''); setMessage(''); setFiles([]);
      } else {
        setError('No se pudo enviar el correo. Comprueba los destinatarios e inténtalo nuevamente. Si el problema continúa, contacta al administrador.');
      }
      await refresh();
    } catch {
      setError('No se pudo enviar el correo. Comprueba los destinatarios e inténtalo nuevamente.');
      await refresh();
    } finally { setBusy(false); }
  };

  return <div className="page-stack notification-center">
    <SectionTitle kicker="COMUNICACIÓN / 07" title="Notificaciones por correo" description="Envía avisos a uno o varios destinatarios y consulta el resultado de cada envío." action={<button type="button" className="button button-secondary" onClick={refresh} disabled={loading}><Icon name="refresh-cw" size={15}/>{loading ? 'Actualizando…' : 'Actualizar historial'}</button>}/>

    <section className="panel mail-status-panel">
      <div className="mail-status-icon"><Icon name="mail" size={22}/></div>
      <div className="mail-status-copy"><strong>Estado del correo</strong><span>{mailStatus?.configurado ? 'El servicio de correo está disponible.' : 'El envío de correos no está disponible en este momento. Contacta al administrador.'}</span></div>
      <Pill tone={mailStatus?.configurado ? 'success' : 'warning'}>{mailStatus?.configurado ? 'Configurado' : 'Pendiente de configurar'}</Pill>
    </section>

    <section className="panel">
      <div className="panel-heading"><div><h2>Enviar notificación</h2><p>Escribe el aviso y adjunta los documentos que necesites compartir.</p></div><Pill tone="info">Solo administrador</Pill></div>
      <form className="form-body" onSubmit={send}>
        <div className="field"><label htmlFor="mail-recipients">Destinatarios *</label><textarea id="mail-recipients" value={recipients} onChange={(e) => setRecipients(e.target.value)} rows="3" maxLength="4000" placeholder="correo1@dominio.com, correo2@dominio.com" required/><small>Separa las direcciones con coma, punto y coma o espacios. Máximo 30 destinatarios.</small></div>
        <div className="field"><label htmlFor="mail-subject">Asunto *</label><input id="mail-subject" value={subject} onChange={(e) => setSubject(e.target.value)} maxLength="180" placeholder="Asunto de la notificación" required/></div>
        <div className="field"><label htmlFor="mail-message">Mensaje *</label><textarea id="mail-message" value={message} onChange={(e) => setMessage(e.target.value)} rows="7" maxLength="20000" placeholder="Escribe el aviso que recibirán los destinatarios…" required/></div>
        <div className="field"><label htmlFor="mail-attachments">Archivos adjuntos</label><input id="mail-attachments" type="file" multiple onChange={(e) => setFiles(Array.from(e.target.files || []))}/><small>Hasta 5 archivos; máximo 10 MB por archivo.</small>{files.length > 0 && <div className="mail-file-list">{files.map((file, index) => <div key={`${file.name}-${index}`}><Icon name="paperclip" size={14}/><span>{file.name}</span><small>{(file.size / 1024 / 1024).toFixed(2)} MB</small></div>)}</div>}</div>
        {error && <div className="auth-error" role="alert"><Icon name="alert" size={16}/>{error}</div>}
        <div className="form-footer"><span>El resultado del intento quedará registrado en el historial de notificaciones.</span><button type="submit" className="button button-primary" disabled={busy || !mailStatus?.configurado}><Icon name="send" size={16}/>{busy ? 'Enviando correo…' : 'Enviar notificación'}</button></div>
        {!mailStatus?.configurado && <p className="inline-note"><Icon name="info" size={15}/> El envío de correos está temporalmente deshabilitado. Contacta al administrador para habilitarlo.</p>}
      </form>
    </section>

    <section className="panel">
      <div className="panel-heading"><div><h2>Historial de notificaciones</h2><p>{history.length} envíos registrados.</p></div></div>
      {loading && <div className="loading-inline"><Icon name="loader-circle" size={17}/> Consultando el historial…</div>}
      {history.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Fecha</th><th>Destinatarios</th><th>Asunto</th><th>Adjuntos</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>{history.map((item) => <tr key={item.idCorreo}><td>{when(item.fechaEnvio)}</td><td>{item.destinatarios}</td><td>{item.asunto}</td><td>{item.nombresAdjuntos || '—'}</td><td><Pill tone={item.estado === 'ENVIADO' ? 'success' : item.estado === 'ERROR' ? 'danger' : 'warning'}>{item.estado}</Pill></td><td>{item.estado === 'ERROR' ? 'No se pudo completar el envío.' : `Enviado por ${item.remitente || 'Administrador'}`}</td></tr>)}</tbody></table></div> : !loading && <EmptyState title="Todavía no hay notificaciones enviadas" detail="Cuando envíes un correo desde este formulario, aparecerá aquí con su fecha, destinatarios, adjuntos y resultado."/>}
    </section>
  </div>;
}
