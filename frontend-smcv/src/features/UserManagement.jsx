import { useCallback, useEffect, useMemo, useState } from 'react';
import Icon from '../components/Icon.jsx';
import { apiRequest } from '../api.js';
import { exportPdf, exportXlsx } from '../utils/exporters.js';

const blankForm = { nombre: '', apellido: '', correo: '', usuarioLogin: '', telefono: '', idRol: '', idArea: null, password: '' };
export default function UserManagement({ notify }) {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [areas, setAreas] = useState([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(blankForm);
  const [createMode, setCreateMode] = useState(false);
  const [resetUser, setResetUser] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      // La lista principal no depende de que también respondan los catálogos de roles y áreas.
      // Antes, un fallo secundario en cualquiera de esos dos endpoints dejaba vacía toda la pantalla.
      const [userResult, roleResult, areaResult] = await Promise.allSettled([
        apiRequest('/api/usuarios/todos'),
        apiRequest('/api/roles'),
        apiRequest('/api/areas?estado=ACTIVO'),
      ]);
      if (userResult.status === 'rejected') throw userResult.reason;
      const userPayload = userResult.value;
      const userList = Array.isArray(userPayload)
        ? userPayload
        : (userPayload?.content || userPayload?.data?.content || userPayload?.usuarios || userPayload?.items || userPayload?.data || null);
      if (!Array.isArray(userList)) {
        throw new Error('No se pudo cargar la lista de usuarios. Actualiza la pantalla o contacta al administrador.');
      }
      setUsers(userList);
      setRoles(roleResult.status === 'fulfilled'
        ? (Array.isArray(roleResult.value) ? roleResult.value : (roleResult.value?.content || []))
        : []);
      setAreas(areaResult.status === 'fulfilled'
        ? (Array.isArray(areaResult.value) ? areaResult.value : (areaResult.value?.content || []))
        : []);
      if (roleResult.status === 'rejected' || areaResult.status === 'rejected') {
        setError('Los usuarios se cargaron, pero algunas opciones de roles o áreas no están disponibles en este momento.');
      }
    } catch { setError('No se pudieron cargar los usuarios. Comprueba que hayas iniciado sesión como administrador e inténtalo nuevamente.'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    let cancelled = false;
    Promise.resolve().then(() => { if (!cancelled) return load(); });
    return () => { cancelled = true; };
  }, [load]);
  const filtered = useMemo(() => users.filter((u) => (!status || String(u.estado || '').toUpperCase() === status) && `${u.nombre || ''} ${u.apellido || ''} ${u.correo || ''} ${u.usuarioLogin || ''} ${u.rol || ''}`.toLowerCase().includes(query.toLowerCase())), [users, query, status]);
  const activeCount = users.filter((u) => String(u.estado || '').toUpperCase() === 'ACTIVO').length;
  const inactiveCount = users.filter((u) => String(u.estado || '').toUpperCase() === 'INACTIVO').length;
  const adminCount = users.filter((u) => String(u.rol || '').toUpperCase() === 'ADMINISTRADOR').length;
  const standardCount = users.length - adminCount;
  const exportUsers = (format) => {
    const rows = filtered.map((u) => ({ ID:u.idUsuario, Nombre:`${u.nombre || ''} ${u.apellido || ''}`.trim(), Usuario:u.usuarioLogin, Correo:u.correo, Rol:u.rol, Área:u.area || u.nombreArea || '', Estado:u.estado }));
    if (format === 'xlsx') exportXlsx(rows, 'usuarios-y-roles-ptar.xlsx');
    else if (!exportPdf(rows, 'Usuarios y roles — PTAR La Enlozada')) notify('Permite las ventanas emergentes para guardar el PDF.');
  };
  const editUser = (user) => { setCreateMode(false); setEditing(user); setForm({ nombre:user.nombre||'', apellido:user.apellido||'', correo:user.correo||'', usuarioLogin:user.usuarioLogin||'', telefono:user.telefono||'', idRol:String(user.idRol), idArea:user.idArea ?? null, password:'' }); };
  const createUser = () => { setEditing(null); setCreateMode(true); setForm({ ...blankForm, idRol: String(roles.find((r) => r.nombre === 'AUDITOR')?.idRol || roles[0]?.idRol || '') }); };
  const saveUser = async (event) => {
    event.preventDefault(); setError(''); setBusy(true);
    try {
      if (createMode) {
        if (form.password.length < 8) throw new Error('La contraseña inicial debe tener al menos 8 caracteres.');
        await apiRequest('/api/usuarios', { method:'POST', body:JSON.stringify({ nombre:form.nombre, apellido:form.apellido, correo:form.correo, usuarioLogin:form.usuarioLogin, telefono:form.telefono || null, idRol:Number(form.idRol), idArea:form.idArea, password:form.password }) });
        notify('Usuario creado correctamente.');
      } else {
        await apiRequest(`/api/usuarios/${editing.idUsuario}`, { method:'PUT', body:JSON.stringify({ nombre:form.nombre, apellido:form.apellido, correo:form.correo, telefono:form.telefono || null, idRol:Number(form.idRol), idArea:form.idArea }) });
        notify('Usuario actualizado correctamente.');
      }
      setEditing(null); setCreateMode(false); await load();
    } catch (e) { setError(e.message || 'No se pudo guardar el usuario.'); }
    finally { setBusy(false); }
  };
  const changeStatus = async (user) => {
    const estado = user.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    if (!window.confirm(`¿Deseas ${estado === 'INACTIVO' ? 'desactivar' : 'activar'} la cuenta de ${user.nombre} ${user.apellido}?`)) return;
    try { await apiRequest(`/api/usuarios/${user.idUsuario}/estado`, { method:'PATCH', body:JSON.stringify({ estado }) }); notify(`Cuenta ${estado === 'ACTIVO' ? 'activada' : 'desactivada'}.`); await load(); }
    catch (e) { notify(`No se pudo cambiar el estado: ${e.message}`); }
  };
  const reset = async (event) => {
    event.preventDefault();
    if (newPassword !== confirmPassword) { setError('Las contraseñas no coinciden.'); return; }
    if (newPassword.length < 8) { setError('La contraseña debe tener al menos 8 caracteres.'); return; }
    setBusy(true); setError('');
    try { await apiRequest(`/api/usuarios/${resetUser.idUsuario}/password`, { method:'PUT', body:JSON.stringify({ passwordNueva:newPassword }) }); notify('Contraseña temporal/restablecida por el administrador. Comunícala al usuario de forma privada.'); setResetUser(null);setNewPassword('');setConfirmPassword(''); }
    catch(e){setError(e.message || 'No se pudo restablecer la contraseña.');}
    finally{setBusy(false);}
  };

  return <div className="page-stack user-management">
    <div className="section-title"><div><span className="eyebrow">ADMINISTRACIÓN / SEGURIDAD</span><h1>Gestión de usuarios</h1><p>Solo el rol ADMINISTRADOR puede consultar y modificar las cuentas. Se incluyen usuarios y administradores, tanto activos como inactivos.</p></div><button className="button button-primary" onClick={createUser}><Icon name="user-plus" size={16}/> Nuevo usuario</button></div>
    <section className="panel">
      <div className="panel-heading"><div><h2>Usuarios y roles</h2><p>{users.length} cuentas registradas. Las contraseñas se mantienen protegidas.</p></div><div className="export-actions"><button className="button button-secondary button-small" onClick={()=>exportUsers('pdf')}><Icon name="file" size={14}/> PDF</button><button className="button button-secondary button-small" onClick={()=>exportUsers('xlsx')}><Icon name="file-spreadsheet" size={14}/> Excel</button><button className="button button-secondary button-small" onClick={load}><Icon name="refresh-cw" size={15}/> Actualizar</button></div></div><div className="user-count-grid"><article><small>Total de cuentas</small><strong>{users.length}</strong></article><article><small>Activas</small><strong>{activeCount}</strong></article><article><small>Desactivadas</small><strong>{inactiveCount}</strong></article><article><small>Administradores</small><strong>{adminCount}</strong></article><article><small>Usuarios con otros roles</small><strong>{standardCount}</strong></article></div>
      <div className="user-table-controls"><label className="search-control"><Icon name="search" size={16}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Buscar nombre, correo o usuario…"/></label><label className="field compact-field"><span>Estado</span><select value={status} onChange={(e)=>setStatus(e.target.value)}><option value="">Todos</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option></select></label></div>
      {error && <div className="auth-error" role="alert">{error}</div>}
      {loading ? <div className="loading-inline"><Icon name="loader-circle" size={18}/> Cargando usuarios…</div> : filtered.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Usuario</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((user)=><tr key={user.idUsuario}><td><strong>{user.nombre} {user.apellido}</strong><small className="table-subline">@{user.usuarioLogin}</small></td><td>{user.correo}</td><td><span className="user-role-pill">{user.rol}</span></td><td><span className={`status-dot-pill ${String(user.estado || '').toUpperCase()==='ACTIVO'?'status-active':'status-inactive'}`}>{user.estado || 'SIN ESTADO'}</span></td><td><div className="table-actions"><button className="button button-secondary button-small" onClick={()=>editUser(user)} title="Editar"><Icon name="edit" size={14}/> Editar</button><button className="button button-ghost button-small" onClick={()=>{setError('');setResetUser(user);setNewPassword('');setConfirmPassword('');}} title="Restablecer contraseña"><Icon name="key-round" size={14}/> Contraseña</button><button className="button button-ghost button-small" onClick={()=>changeStatus(user)} title="Activar o desactivar"><Icon name={String(user.estado || '').toUpperCase()==='ACTIVO'?'user-x':'user-check'} size={14}/>{String(user.estado || '').toUpperCase()==='ACTIVO'?'Desactivar':'Activar'}</button></div></td></tr>)}</tbody></table></div> : <div className="empty-state"><Icon name="users" size={24}/><strong>No hay usuarios para mostrar</strong><span>No hay cuentas para mostrar. Comprueba que existan usuarios registrados y pulsa Actualizar.</span></div>}
    </section>
    {(editing || createMode) && <div className="modal-backdrop" role="presentation" onMouseDown={(e)=>{if(e.target===e.currentTarget){setEditing(null);setCreateMode(false);}}}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="user-editor-title"><div className="modal-heading"><div><span className="eyebrow">SEGURIDAD Y PERMISOS</span><h2 id="user-editor-title">{createMode?'Crear usuario':'Editar usuario'}</h2><p>El servidor valida correo único, roles activos y reglas de administrador.</p></div><button className="icon-button" onClick={()=>{setEditing(null);setCreateMode(false);}} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" onSubmit={saveUser}><div className="form-grid"><div className="field"><label>Nombre</label><input maxLength="80" required value={form.nombre} onChange={(e)=>setForm({...form,nombre:e.target.value})}/></div><div className="field"><label>Apellido</label><input maxLength="80" required value={form.apellido} onChange={(e)=>setForm({...form,apellido:e.target.value})}/></div><div className="field field-span-2"><label>Correo electrónico</label><input type="email" maxLength="120" required value={form.correo} onChange={(e)=>setForm({...form,correo:e.target.value})}/></div>{createMode&&<div className="field field-span-2"><label>Nombre de usuario</label><input minLength="3" maxLength="40" required value={form.usuarioLogin} onChange={(e)=>setForm({...form,usuarioLogin:e.target.value})}/></div>}<div className="field"><label>Teléfono</label><input maxLength="20" value={form.telefono} onChange={(e)=>setForm({...form,telefono:e.target.value})}/></div><div className="field"><label>Rol</label><select required value={form.idRol} onChange={(e)=>setForm({...form,idRol:e.target.value})}>{roles.map((r)=><option value={r.idRol} key={r.idRol}>{r.nombre}</option>)}</select></div><div className="field"><label>Área (opcional)</label><select value={form.idArea == null ? '' : String(form.idArea)} onChange={(e)=>setForm({...form,idArea:e.target.value ? Number(e.target.value) : null})}><option value="">Sin área asignada</option>{areas.map((a)=><option value={a.idArea} key={a.idArea}>{a.nombreArea}</option>)}</select></div>{createMode&&<div className="field field-span-2"><label>Contraseña inicial</label><input type="password" minLength="8" maxLength="72" required value={form.password} onChange={(e)=>setForm({...form,password:e.target.value})}/></div>}</div>{error&&<div className="auth-error" role="alert">{error}</div>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={()=>{setEditing(null);setCreateMode(false);}}>Cancelar</button><button className="button button-primary" type="submit" disabled={busy}>{busy?'Guardando…':'Guardar'} <Icon name="check" size={16}/></button></div></form></section></div>}
    {resetUser && <div className="modal-backdrop" role="presentation" onMouseDown={(e)=>{if(e.target===e.currentTarget)setResetUser(null);}}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="reset-user-title"><div className="modal-heading"><div><span className="eyebrow">RESTABLECIMIENTO ADMINISTRATIVO</span><h2 id="reset-user-title">Nueva contraseña</h2><p>Cuenta: {resetUser.correo}. Comparte la contraseña por un canal privado.</p></div><button className="icon-button" onClick={()=>setResetUser(null)} aria-label="Cerrar"><Icon name="close"/></button></div><form className="form-body" data-match-fields="admin-reset-password,admin-reset-password-confirm" onSubmit={reset}><div className="field"><label>Nueva contraseña</label><input id="admin-reset-password" type="password" minLength="8" maxLength="72" autoComplete="new-password" value={newPassword} onChange={(e)=>setNewPassword(e.target.value)} required/></div><div className="field"><label>Confirmar nueva contraseña</label><input id="admin-reset-password-confirm" type="password" minLength="8" maxLength="72" autoComplete="new-password" value={confirmPassword} onChange={(e)=>setConfirmPassword(e.target.value)} required/></div>{error&&<div className="auth-error" role="alert">{error}</div>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={()=>setResetUser(null)}>Cancelar</button><button type="submit" className="button button-primary" disabled={busy}>{busy?'Guardando…':'Restablecer contraseña'}</button></div></form></section></div>}
  </div>;
}
