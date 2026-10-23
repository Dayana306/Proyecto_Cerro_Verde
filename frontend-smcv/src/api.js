const API_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8081').replace(/\/$/, '');

export function getToken() { return localStorage.getItem('smcv_token'); }
export function clearSession() {
  localStorage.removeItem('smcv_token');
  localStorage.removeItem('smcv_user');
}
export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (response.status === 401 && !path.includes('/api/auth/login')) {
    clearSession();
    window.dispatchEvent(new Event('smcv:session-expired'));
  }
  if (!response.ok) {
    let message = `Error HTTP ${response.status}`;
    try { const body = await response.json(); message = body.message || body.error || message; } catch { /* respuesta sin JSON */ }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  const type = response.headers.get('content-type') || '';
  return type.includes('application/json') ? response.json() : response;
}
export async function login(correo, password) {
  const result = await apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify({ correo, password }) });
  if (!result?.token || !result?.usuario) throw new Error('La respuesta de inicio de sesión no contiene la sesión esperada.');
  localStorage.setItem('smcv_token', result.token);
  localStorage.setItem('smcv_user', JSON.stringify(result.usuario));
  return result.usuario;
}
export async function registerAccount(payload) { return apiRequest('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }); }
export async function currentUser() { return apiRequest('/api/auth/me'); }
export function apiBaseUrl() { return API_BASE; }
