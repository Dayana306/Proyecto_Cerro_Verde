import { useEffect, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { apiRequest } from '../api.js';

async function imageAsDataUrl(file) {
  if (!file || !file.type.startsWith('image/')) throw new Error('Selecciona un archivo de imagen válido.');
  if (file.size > 8 * 1024 * 1024) throw new Error('La imagen original no puede superar 8 MB.');
  const objectUrl = URL.createObjectURL(file);
  try {
    const image = new Image();
    image.src = objectUrl;
    await image.decode();
    const max = 520;
    const scale = Math.min(1, max / Math.max(image.naturalWidth, image.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
    const context = canvas.getContext('2d');
    if (!context) throw new Error('No se pudo procesar la imagen en este navegador.');
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.82);
  } finally { URL.revokeObjectURL(objectUrl); }
}

export default function AccountSettings({ notify, onProfileUpdated, isAdmin = false, onManageUsers }) {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ nombre: '', apellido: '', telefono: '', fotoPerfil: null });
  const [password, setPassword] = useState({ actual: '', nueva: '', confirmar: '' });
  const [busy, setBusy] = useState(false);
  const [passwordBusy, setPasswordBusy] = useState(false);
  const [error, setError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  useEffect(() => {
    let cancelled = false;
    apiRequest('/api/perfil').then((data) => {
      if (cancelled) return;
      setProfile(data);
      setForm({ nombre: data.nombre || '', apellido: data.apellido || '', telefono: data.telefono || '', fotoPerfil: data.fotoPerfil || null });
      onProfileUpdated?.(data);
    }).catch((e) => { if (!cancelled) setError(e.message || 'No se pudo cargar el perfil.'); });
    return () => { cancelled = true; };
  }, [onProfileUpdated]);

  const submitProfile = async (event) => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      const saved = await apiRequest('/api/perfil', { method: 'PUT', body: JSON.stringify(form) });
      setProfile(saved); setForm({ nombre: saved.nombre || '', apellido: saved.apellido || '', telefono: saved.telefono || '', fotoPerfil: saved.fotoPerfil || null });
      onProfileUpdated?.(saved); notify('Perfil actualizado en la base de datos.');
    } catch (e) { setError(e.message || 'No se pudo guardar el perfil.'); }
    finally { setBusy(false); }
  };
  const selectPhoto = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setError('');
    try { const fotoPerfil = await imageAsDataUrl(file); setForm((old) => ({ ...old, fotoPerfil })); }
    catch (e) { setError(e.message || 'No se pudo cargar la foto.'); }
    event.target.value = '';
  };
  const submitPassword = async (event) => {
    event.preventDefault(); setPasswordError('');
    if (password.nueva !== password.confirmar) { setPasswordError('La nueva contraseña y su confirmación no coinciden.'); return; }
    if (password.nueva.length < 8 || password.nueva.length > 72) { setPasswordError('Usa una contraseña de entre 8 y 72 caracteres.'); return; }
    setPasswordBusy(true);
    try {
      await apiRequest('/api/auth/password', { method: 'PUT', body: JSON.stringify({ passwordActual: password.actual, passwordNueva: password.nueva }) });
      setPassword({ actual: '', nueva: '', confirmar: '' }); notify('Contraseña cambiada correctamente.');
    } catch (e) { setPasswordError(e.message || 'No se pudo cambiar la contraseña.'); }
    finally { setPasswordBusy(false); }
  };

  return <div className="page-stack account-settings">
    <div className="section-title"><div><span className="eyebrow">CUENTA / SEGURIDAD</span><h1>Mi perfil</h1><p>Actualiza tu información personal y protege el acceso a tu cuenta.</p></div></div>
    <section className="panel account-profile-panel">
      <div className="panel-heading"><div><h2>Mi perfil</h2><p>La cuenta, el correo y el rol se verifican en el servidor.</p></div><Icon name="user-round" size={22}/></div>
      {error && <div className="auth-error" role="alert">{error}</div>}
      <form className="form-body" onSubmit={submitProfile}>
        <div className="profile-photo-row">
          <div className="profile-photo-preview">{form.fotoPerfil ? <img src={form.fotoPerfil} alt="Foto de perfil"/> : <Icon name="user-round" size={40}/>}</div>
          <div><strong>Foto de perfil (opcional)</strong><p>JPG, PNG o WebP. La imagen se reduce automáticamente antes de guardarse.</p><label className="button button-secondary button-small photo-upload-label"><Icon name="image-plus" size={15}/> Elegir imagen<input type="file" accept="image/jpeg,image/png,image/webp" onChange={selectPhoto} hidden/></label>{form.fotoPerfil && <button type="button" className="button button-ghost button-small" onClick={() => setForm((old) => ({ ...old, fotoPerfil: null }))}>Quitar foto</button>}</div>
        </div>
        <div className="form-grid">
          <div className="field"><label htmlFor="profile-nombre">Nombres</label><input id="profile-nombre" maxLength="80" required value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })}/></div>
          <div className="field"><label htmlFor="profile-apellido">Apellidos</label><input id="profile-apellido" maxLength="80" required value={form.apellido} onChange={(e) => setForm({ ...form, apellido: e.target.value })}/></div>
          <div className="field"><label htmlFor="profile-correo">Correo electrónico</label><input id="profile-correo" value={profile?.correo || ''} readOnly disabled/></div>
          <div className="field"><label htmlFor="profile-login">Nombre de usuario</label><input id="profile-login" value={profile?.usuarioLogin || ''} readOnly disabled/></div>
          <div className="field"><label htmlFor="profile-telefono">Teléfono</label><input id="profile-telefono" maxLength="20" value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} placeholder="Opcional"/></div>
          <div className="field"><label htmlFor="profile-role">Rol asignado</label><input id="profile-role" value={profile?.rol || ''} readOnly disabled/></div>
        </div>
        <div className="form-footer"><span>Los cambios se guardan en el servidor.</span><button type="submit" className="button button-primary" disabled={busy}>{busy ? 'Guardando…' : 'Guardar perfil'} <Icon name="check" size={16}/></button></div>
      </form>
    </section>
    <section className="panel">
      <div className="panel-heading"><div><h2>Cambiar contraseña</h2><p>Necesitas tu contraseña actual para modificarla desde la sesión.</p></div><Icon name="lock-keyhole" size={22}/></div>
      <form className="form-body" onSubmit={submitPassword}>
        <div className="form-grid">
          <div className="field field-span-2"><label htmlFor="password-actual">Contraseña actual</label><input id="password-actual" type="password" autoComplete="current-password" value={password.actual} onChange={(e) => setPassword({ ...password, actual: e.target.value })} required/></div>
          <div className="field"><label htmlFor="password-nueva">Nueva contraseña</label><input id="password-nueva" type="password" minLength="8" maxLength="72" autoComplete="new-password" value={password.nueva} onChange={(e) => setPassword({ ...password, nueva: e.target.value })} required/><small>Mínimo 8 caracteres.</small></div>
          <div className="field"><label htmlFor="password-confirmar">Confirmar nueva contraseña</label><input id="password-confirmar" type="password" minLength="8" maxLength="72" autoComplete="new-password" value={password.confirmar} onChange={(e) => setPassword({ ...password, confirmar: e.target.value })} required/></div>
        </div>
        {passwordError && <div className="auth-error" role="alert">{passwordError}</div>}
        <div className="form-footer"><span>Las contraseñas se almacenan con BCrypt; nunca se muestra el hash.</span><button className="button button-primary" type="submit" disabled={passwordBusy}>{passwordBusy ? 'Actualizando…' : 'Cambiar contraseña'} <Icon name="shield-check" size={16}/></button></div>
      </form>
    </section>
  </div>;
}
