--Roles del sistema-los nombres deben coincidir con los usados en @PreAuthorize.
INSERT INTO rol (nombre, descripcion) VALUES
  ('ADMINISTRADOR', 'Gestiona usuarios, roles, estaciones y umbrales de alerta'),
  ('SUPERVISOR',    'Supervisor de planta: registra parámetros hídricos'),
  ('GERENTE',       'Gerente de Medio Ambiente: dashboard, alertas y reportes'),
  ('AUDITOR',       'Auditor ANA/OEFA: acceso de solo lectura');
