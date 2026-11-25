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
    const safeFallback = 'No se pudo completar la operación. Verifica los datos e inténtalo nuevamente. Si el problema continúa, contacta al administrador.';
    let message = `No se pudo completar la operación (${response.status}).`;
    try {
      const body = await response.json();
      message = body.message || body.error || body.errorResumen || message;
    } catch { /* respuesta sin JSON */ }
    const technicalDetails = /postgres|psql|sqlstate|\bsql\b|jdbc|jpa|hibernate|flyway|spring(?:framework|boot)?|jakarta\.persistence|exception|stack[\s-]?trace|constraint|relation|column|syntax error|could not execute|database|base de datos|duplicate key|violates|no static resource|whitelabel|internal server error|org\.[\w.$]+|java\.[\w.$]+/i;
    if (technicalDetails.test(String(message))) message = safeFallback;
    throw new Error(message || safeFallback);
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

/** Descarga archivos protegidos por JWT sin exponer el token en una URL. */
export async function downloadProtectedFile(path, fallbackName = 'archivo') {
  const response = await apiRequest(path);
  if (!(response instanceof Response)) throw new Error('La descarga no devolvió un archivo válido.');
  const blob = await response.blob();
  const disposition = response.headers.get('content-disposition') || '';
  let name = fallbackName;
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  const plain = disposition.match(/filename="?([^";]+)"?/i);
  try { if (encoded) name = decodeURIComponent(encoded[1]); else if (plain) name = plain[1]; } catch { /* usa el nombre de respaldo */ }
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a'); link.href = url; link.download = name;
  document.body.appendChild(link); link.click(); link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  return name;
}
