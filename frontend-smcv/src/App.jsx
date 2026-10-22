import React, { useState } from 'react';
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement,
  LineElement, ArcElement, Title, Tooltip, Legend,
} from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Title, Tooltip, Legend);

export default function App() {
  const [autenticado, setAutenticado] = useState(false);
  const [vistaActual, setVistaActual] = useState('dashboard');
  const [rolUsuario, setRolUsuario] = useState('Admin');

  // Estado del formulario de Registro (RF-02)
  const [formRegistro, setFormRegistro] = useState({
    estacion: 'P02 - Río Principal', fecha: '2026-08-26', hora: '10:30',
    ph: '', turbidez: '', temperatura: '', caudal: '', observacion: ''
  });
  const [mensajeAlerta, setMensajeAlerta] = useState(null);

  // Gráficos (Figura 3 del informe)
  const dataLineas = {
    labels: ['20/08', '21/08', '22/08', '23/08', '24/08', '25/08', '26/08'],
    datasets: [
      { label: 'Caudal tratado (m³/s)', data: [1.35, 1.42, 1.38, 1.49, 1.58, 1.46, 1.52], borderColor: '#0d6efd', backgroundColor: '#0d6efd', tension: 0.3 },
      { label: 'Límite Convenio Sedapar (1.50 m³/s)', data: [1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5], borderColor: '#dc3545', borderDash: [5, 5], pointRadius: 0 },
    ],
  };

  const dataDona = {
    labels: ['Bueno (60%)', 'Regular (25%)', 'Malo (10%)', 'Crítico (5%)'],
    datasets: [{ data: [60, 25, 10, 5], backgroundColor: ['#198754', '#ffc107', '#fd7e14', '#dc3545'] }],
  };

  // Simulación de validación (RF-03) y generación de alertas (RF-05)
  const manejarEnvioRegistro = (e) => {
    e.preventDefault();
    const phNum = parseFloat(formRegistro.ph);
    const caudalNum = parseFloat(formRegistro.caudal);
    const turbidezNum = parseFloat(formRegistro.turbidez);

    if (phNum < 0 || caudalNum < 0 || turbidezNum < 0) {
      setMensajeAlerta({ tipo: 'danger', texto: 'Error RF-03: Los valores numéricos no pueden ser negativos.' });
      return;
    }
    if (caudalNum > 1.5 || turbidezNum > 25 || phNum < 6.0 || phNum > 9.0) {
      setMensajeAlerta({ tipo: 'warning', texto: `¡ALERTA AUTOMÁTICA GENERADA (RF-05)! Parámetro fuera de umbral en ${formRegistro.estacion}. Notificado a Gerencia de Medio Ambiente.` });
    } else {
      setMensajeAlerta({ tipo: 'success', texto: 'Registro consolidado correctamente dentro de los límites permitidos (RF-02).' });
    }
  };

  // VISTA DE LOGIN (RF-01)
  if (!autenticado) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 bg-light">
        <div className="card shadow p-4" style={{ width: '380px' }}>
          <div className="text-center mb-4">
            <div className="bg-dark text-white rounded-circle d-inline-flex align-items-center justify-content-center mb-2" style={{ width: '60px', height: '60px', fontSize: '24px' }}>💧</div>
            <h4 className="fw-bold mb-0">Cerro Verde</h4>
            <small className="text-muted">Sistema Web de Monitoreo Hídrico</small>
          </div>
          <form onSubmit={(e) => { e.preventDefault(); setAutenticado(true); }}>
            <div className="mb-3">
              <label className="form-label fw-semibold">Correo / Usuario</label>
              <input type="text" className="form-control" placeholder="admin@cerroverde.pe" required />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold">Contraseña</label>
              <input type="password" className="form-control" placeholder="••••••••" required />
            </div>
            <div className="mb-4">
              <label className="form-label fw-semibold">Rol de Acceso</label>
              <select className="form-select" value={rolUsuario} onChange={(e) => setRolUsuario(e.target.value)}>
                <option value="Administrador">Administrador del Sistema</option>
                <option value="Supervisor">Supervisor de Planta</option>
                <option value="Gerente">Gerente de Medio Ambiente</option>
                <option value="Auditor">Auditor (ANA / OEFA)</option>
              </select>
            </div>
            <button type="submit" className="btn btn-dark w-100 fw-bold">INICIAR SESIÓN</button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="d-flex" style={{ minHeight: '100vh' }}>
      {/* SIDEBAR */}
      <div className="bg-dark text-white p-3 d-flex flex-column" style={{ width: '260px' }}>
        <div className="d-flex align-items-center mb-4 pb-3 border-bottom border-secondary">
          <span className="fs-3 me-3">⛰️</span>
          <div>
            <h6 className="mb-0 fw-bold">Cerro Verde</h6>
            <small className="text-secondary" style={{ fontSize: '12px' }}>PTAR La Enlozada</small>
          </div>
        </div>
        <ul className="nav nav-pills flex-column mb-auto gap-2">
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'dashboard' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('dashboard')}>📊 Dashboard</button></li>
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'monitoreo' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('monitoreo')}>📝 Monitoreo (Registro)</button></li>
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'estaciones' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('estaciones')}>📍 Puntos / Estaciones</button></li>
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'mapa2d' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('mapa2d')}>🗺️ Mapa 2D (Impacto)</button></li>
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'alertas' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('alertas')}>🔔 Alertas</button></li>
          <li><button className={`nav-link text-white w-100 text-start ${vistaActual === 'auditoria' ? 'active bg-secondary' : ''}`} onClick={() => setVistaActual('auditoria')}>📄 Reportes y Auditoría</button></li>
        </ul>
        <button className="btn btn-outline-light btn-sm mt-4 fw-bold" onClick={() => setAutenticado(false)}>Cerrar sesión</button>
      </div>

      {/* CONTENIDO PRINCIPAL */}
      <div className="flex-grow-1 bg-light">
        <header className="bg-white shadow-sm px-4 py-3 d-flex justify-content-between align-items-center mb-4">
          <h5 className="mb-0 fw-bold text-dark">Monitoreo Hídrico PTAR La Enlozada</h5>
          <span className="badge bg-dark px-3 py-2 fs-6">Usuario: {rolUsuario}</span>
        </header>

        <main className="px-4 pb-4">
          {/* VISTA 1: DASHBOARD GENERAL */}
          {vistaActual === 'dashboard' && (
            <div>
              <h5 className="fw-bold mb-3 text-secondary">DASHBOARD GENERAL</h5>
              <div className="row g-3 mb-4">
                <div className="col-md"><div className="card text-center p-3 shadow-sm border-0"><small className="text-muted fw-bold">PUNTOS</small><h2 className="fw-bold my-1">5</h2><small className="text-success">Activos</small></div></div>
                <div className="col-md"><div className="card text-center p-3 shadow-sm border-0"><small className="text-muted fw-bold">pH PROMEDIO</small><h2 className="fw-bold my-1">7.15</h2><small className="text-secondary">(Normal)</small></div></div>
                <div className="col-md"><div className="card text-center p-3 shadow-sm border-0"><small className="text-muted fw-bold">TURBIDEZ</small><h2 className="fw-bold my-1">11.8 NTU</h2><small className="text-success">(Bueno)</small></div></div>
                <div className="col-md"><div className="card text-center p-3 shadow-sm border-0"><small className="text-muted fw-bold">CAUDAL ACTUAL</small><h2 className="fw-bold my-1 text-danger">1.52 m³/s</h2><small className="text-danger">(Sobre límite 1.5)</small></div></div>
                <div className="col-md"><div className="card text-center p-3 shadow-sm border-0"><small className="text-muted fw-bold">ALERTAS</small><h2 className="fw-bold my-1 text-warning">3</h2><small className="text-danger">(2 Altas)</small></div></div>
              </div>
              <div className="row g-4 mb-4">
                <div className="col-md-8"><div className="card p-3 shadow-sm border-0 h-100"><h6 className="fw-bold text-secondary">TENDENCIA DE CAUDAL TRATADO (m³/s)</h6><Line data={dataLineas} /></div></div>
                <div className="col-md-4"><div className="card p-3 shadow-sm border-0 h-100"><h6 className="fw-bold text-secondary">DISTRIBUCIÓN DE ESTADOS</h6><Doughnut data={dataDona} /></div></div>
              </div>
            </div>
          )}

          {/* VISTA 2: REGISTRO */}
          {vistaActual === 'monitoreo' && (
            <div className="card p-4 shadow-sm border-0 mx-auto" style={{ maxWidth: '750px' }}>
              <h5 className="fw-bold mb-4 text-secondary">Registro Manual de Parámetros Hídricos</h5>
              {mensajeAlerta && (<div className={`alert alert-${mensajeAlerta.tipo} fw-semibold`}>{mensajeAlerta.texto}</div>)}
              <form onSubmit={manejarEnvioRegistro}>
                <div className="row g-3">
                  <div className="col-md-6">
                    <label className="form-label fw-bold text-muted">Estación (Punto de control)</label>
                    <select className="form-select" value={formRegistro.estacion} onChange={(e) => setFormRegistro({ ...formRegistro, estacion: e.target.value })}>
                      <option>P01 - Bocatoma</option><option>P02 - Río Principal</option><option>P03 - Salida PTAR</option>
                    </select>
                  </div>
                  <div className="col-md-3">
                    <label className="form-label fw-bold text-muted">Fecha</label>
                    <input type="date" className="form-control" value={formRegistro.fecha} onChange={(e) => setFormRegistro({ ...formRegistro, fecha: e.target.value })} required />
                  </div>
                  <div className="col-md-3">
                    <label className="form-label fw-bold text-muted">Hora</label>
                    <input type="time" className="form-control" value={formRegistro.hora} onChange={(e) => setFormRegistro({ ...formRegistro, hora: e.target.value })} required />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-bold text-muted">pH (Normal: 6.0 - 9.0)</label>
                    <input type="number" step="0.01" className="form-control" placeholder="Ej. 7.2" value={formRegistro.ph} onChange={(e) => setFormRegistro({ ...formRegistro, ph: e.target.value })} required />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-bold text-muted">Caudal en m³/s (Límite: 1.50)</label>
                    <input type="number" step="0.01" className="form-control" placeholder="Ej. 1.45" value={formRegistro.caudal} onChange={(e) => setFormRegistro({ ...formRegistro, caudal: e.target.value })} required />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-bold text-muted">Turbidez en NTU (Máx: 25.0)</label>
                    <input type="number" step="0.1" className="form-control" placeholder="Ej. 12.5" value={formRegistro.turbidez} onChange={(e) => setFormRegistro({ ...formRegistro, turbidez: e.target.value })} required />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-bold text-muted">Temperatura (°C)</label>
                    <input type="number" step="0.1" className="form-control" placeholder="Ej. 18.5" value={formRegistro.temperatura} onChange={(e) => setFormRegistro({ ...formRegistro, temperatura: e.target.value })} required />
                  </div>
                </div>
                <button type="submit" className="btn btn-dark mt-4 px-4 fw-bold w-100">Guardar Registro en Base de Datos</button>
              </form>
            </div>
          )}

          {/* VISTA 3: PUNTOS ESTACIONES */}
          {vistaActual === 'estaciones' && (
            <div className="card p-4 shadow-sm border-0">
              <h5 className="fw-bold mb-4 text-secondary">Detalle histórico por estación</h5>
              <table className="table table-bordered table-hover align-middle text-center">
                <thead className="table-dark">
                  <tr><th>ESTACIÓN</th><th>pH PROM.</th><th>TURBIDEZ (NTU)</th><th>CAUDAL (m³/s)</th><th>ALERTAS HISTÓRICAS</th><th>ESTADO ACTUAL</th></tr>
                </thead>
                <tbody>
                  <tr><td className="fw-semibold text-start">P01 - Bocatoma</td><td>7.1</td><td>9.8</td><td>1.52</td><td>1</td><td><span className="badge bg-warning text-dark">Observado</span></td></tr>
                  <tr><td className="fw-semibold text-start">P02 - Río Principal</td><td>7.2</td><td>14.6</td><td>1.49</td><td>3</td><td><span className="badge bg-warning text-dark">Observado</span></td></tr>
                  <tr><td className="fw-semibold text-start">P03 - Salida PTAR</td><td>6.9</td><td>8.1</td><td>1.31</td><td>0</td><td><span className="badge bg-success">Conforme</span></td></tr>
                  <tr><td className="fw-semibold text-start">P04 - Canal derivación</td><td>7.0</td><td>12.0</td><td>1.40</td><td>1</td><td><span className="badge bg-success">Conforme</span></td></tr>
                  <tr><td className="fw-semibold text-start">P05 - Punto de reúso</td><td>7.05</td><td>10.2</td><td>1.46</td><td>1</td><td><span className="badge bg-success">Conforme</span></td></tr>
                </tbody>
              </table>
            </div>
          )}

          {/* VISTA 4: MAPA 2D */}
          {vistaActual === 'mapa2d' && (
            <div>
              <h5 className="fw-bold mb-3 text-secondary">MAPA 2D DE IMPACTO HÍDRICO (RF-08)</h5>
              <div className="row g-4">
                <div className="col-md-7">
                  <div className="card p-3 shadow-sm border-0 bg-dark text-white position-relative" style={{ height: '400px', backgroundImage: 'radial-gradient(#333, #111)' }}>
                    <p className="small text-secondary mb-1">Representación espacial 2D - Cuenca Río Chili</p>
                    <div className="position-absolute bg-danger text-white px-2 py-1 rounded small fw-bold" style={{ top: '30%', left: '25%' }}>📍 P01 - Bocatoma</div>
                    <div className="position-absolute bg-warning text-dark px-2 py-1 rounded small fw-bold shadow-lg" style={{ top: '45%', left: '48%', border: '2px solid white' }}>📍 P02 - Río Principal</div>
                    <div className="position-absolute bg-success text-white px-2 py-1 rounded small fw-bold" style={{ top: '68%', left: '35%' }}>📍 P03 - Salida PTAR</div>
                    <div className="position-absolute text-info fw-bold" style={{ top: '25%', left: '55%' }}>🏘️ Pueblo Nuevo</div>
                    <div className="position-absolute text-info fw-bold" style={{ top: '50%', left: '72%' }}>🏘️ San Miguel</div>
                    <div className="position-absolute text-info fw-bold" style={{ top: '75%', left: '68%' }}>🏘️ El Molino</div>
                    <div className="position-absolute text-info fw-bold" style={{ top: '80%', left: '20%' }}>🏘️ La Esperanza</div>
                  </div>
                </div>
                <div className="col-md-5">
                  <div className="card p-4 shadow-sm border-0 h-100">
                    <h6 className="fw-bold text-dark">IMPACTO EN POBLADOS CERCANOS</h6>
                    <table className="table mt-3">
                      <thead><tr className="text-secondary"><th>Poblado</th><th>Distancia</th><th>Nivel</th></tr></thead>
                      <tbody>
                        <tr><td className="fw-bold">Pueblo Nuevo</td><td>2.1 km</td><td><span className="badge bg-danger">Alto</span></td></tr>
                        <tr><td className="fw-bold">San Miguel</td><td>4.3 km</td><td><span className="badge bg-warning text-dark">Medio</span></td></tr>
                        <tr><td className="fw-bold">El Molino</td><td>6.7 km</td><td><span className="badge bg-success">Bajo</span></td></tr>
                        <tr><td className="fw-bold">La Esperanza</td><td>9.4 km</td><td><span className="badge bg-secondary">Sin impacto</span></td></tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* VISTA 5: ALERTAS */}
          {vistaActual === 'alertas' && (
            <div>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <h5 className="fw-bold mb-0 text-secondary">Panel de Alertas (CU09)</h5>
                <button className="btn btn-outline-dark fw-bold" onClick={() => alert('Parámetros actuales: \npH: 6.0 a 9.0 \nCaudal Máx: 1.50 m³/s \nTurbidez Máx: 25 NTU')}>
                  ⚙️ Configurar Umbrales
                </button>
              </div>
              <div className="d-flex flex-column gap-3">
                <div className="card p-3 shadow-sm border-0 border-start border-danger border-5 d-flex flex-row justify-content-between align-items-center">
                  <div>
                    <h5 className="fw-bold mb-1 text-danger">[ALTA] Turbidez alta detectada en P02</h5>
                    <p className="text-muted mb-0">Valor detectado: <b className="text-dark">32 NTU</b> (Límite: 25 NTU) | 26/08/2026 10:30</p>
                  </div>
                  <span className="badge bg-danger fs-6">Notificada</span>
                </div>
                <div className="card p-3 shadow-sm border-0 border-start border-warning border-5 d-flex flex-row justify-content-between align-items-center">
                  <div>
                    <h5 className="fw-bold mb-1 text-warning text-dark">[MEDIA] pH fuera de rango en P03</h5>
                    <p className="text-muted mb-0">Valor detectado: <b className="text-dark">5.6</b> (Rango: 6.0 - 9.0) | 26/08/2026 09:15</p>
                  </div>
                  <span className="badge bg-warning text-dark fs-6">En revisión</span>
                </div>
                <div className="card p-3 shadow-sm border-0 border-start border-danger border-5 d-flex flex-row justify-content-between align-items-center">
                  <div>
                    <h5 className="fw-bold mb-1 text-danger">[ALTA] Caudal excede convenio en P01</h5>
                    <p className="text-muted mb-0">Valor detectado: <b className="text-dark">1.58 m³/s</b> (Límite: 1.5 m³/s) | 25/08/2026 22:05</p>
                  </div>
                  <span className="badge bg-dark fs-6">Escalada</span>
                </div>
              </div>
            </div>
          )}

          {/* VISTA 6: AUDITORÍA ANA/OEFA */}
          {vistaActual === 'auditoria' && (
            <div>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                  <h5 className="fw-bold mb-1 text-secondary">Auditoría de Cumplimiento Normativo (CU10/CU11)</h5>
                  <p className="text-muted mb-0">Módulo de solo lectura para entidades reguladoras (ANA / OEFA)</p>
                </div>
                <button className="btn btn-dark fw-bold px-4 py-2" onClick={() => window.print()}>
                  📄 EXPORTAR EVIDENCIAS (PDF)
                </button>
              </div>

              <div className="row g-3 mb-4 text-center">
                <div className="col-md-3"><div className="card p-3 border-0 shadow-sm"><small className="text-muted fw-bold">REGISTROS AUDITADOS</small><h2 className="fw-bold mb-0 text-primary">1,248</h2></div></div>
                <div className="col-md-3"><div className="card p-3 border-0 shadow-sm"><small className="text-muted fw-bold">OBSERVACIONES ABIERTAS</small><h2 className="fw-bold mb-0 text-warning">2</h2></div></div>
                <div className="col-md-3"><div className="card p-3 border-0 shadow-sm"><small className="text-muted fw-bold">CUMPLIMIENTO CONVENIO</small><h2 className="fw-bold mb-0 text-success">96.4%</h2></div></div>
                <div className="col-md-3"><div className="card p-3 border-0 shadow-sm"><small className="text-muted fw-bold">VIGENCIA CONVENIO SEDAPAR</small><h2 className="fw-bold mb-0 text-dark">2060</h2></div></div>
              </div>

              <div className="card p-4 shadow-sm border-0">
                <h6 className="fw-bold mb-3">Histórico de eventos auditables (Bitácora RNF-04)</h6>
                <table className="table table-hover align-middle">
                  <thead className="table-light">
                    <tr><th>Fecha y Hora</th><th>Tipo de evento</th><th>Estación</th><th>Usuario / Origen</th><th>Detalle Técnico</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    <tr><td>26/08/2026 10:30</td><td><span className="badge bg-danger">Alerta generada</span></td><td>P02</td><td>Sistema Automático</td><td>Turbidez 32 &gt; 25 NTU</td><td><span className="badge bg-secondary">Notificada</span></td></tr>
                    <tr><td>25/08/2026 22:05</td><td><span className="badge bg-danger">Alerta escalada</span></td><td>P01</td><td>Sistema Automático</td><td>Caudal 1.58 &gt; 1.5 m³/s</td><td><span className="badge bg-dark">Escalada</span></td></tr>
                    <tr><td>25/08/2026 09:00</td><td><span className="badge bg-success">Reporte generado</span></td><td>Todas</td><td>gerencia.ma</td><td>Reporte RA-2026-0041 enviado</td><td><span className="badge bg-primary">Enviado</span></td></tr>
                    <tr><td>24/08/2026 14:12</td><td><span className="badge bg-warning text-dark">Umbral modificado</span></td><td>P03</td><td>admin.smcv</td><td>Turbidez máx: 25 → 22 NTU</td><td><span className="badge bg-success">Auditado</span></td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}