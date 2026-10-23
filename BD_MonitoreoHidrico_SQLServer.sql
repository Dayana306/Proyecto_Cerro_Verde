USE master;
GO

IF DB_ID('MonitoreoHidricoCV') IS NOT NULL
BEGIN
    ALTER DATABASE MonitoreoHidricoCV SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE MonitoreoHidricoCV;
END
GO

CREATE DATABASE MonitoreoHidricoCV;
GO

USE MonitoreoHidricoCV;
GO

/* ROL */
CREATE TABLE dbo.Rol (
    id_rol        INT IDENTITY(1,1) NOT NULL,
    nombre_rol    VARCHAR(50)  NOT NULL,
    descripcion   VARCHAR(200) NULL,
    CONSTRAINT PK_Rol PRIMARY KEY (id_rol),
    CONSTRAINT UQ_Rol_nombre UNIQUE (nombre_rol)
);
GO


   -- USUARIO  

CREATE TABLE dbo.Usuario (
    id_usuario     INT IDENTITY(1,1) NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    correo         VARCHAR(120) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,       -- (nunca texto plano)
    id_rol         INT          NOT NULL,
    CONSTRAINT PK_Usuario PRIMARY KEY (id_usuario),
    CONSTRAINT UQ_Usuario_correo UNIQUE (correo),
    CONSTRAINT FK_Usuario_Rol FOREIGN KEY (id_rol)
        REFERENCES dbo.Rol (id_rol) ON DELETE NO ACTION
);
GO


   --ESTACION_MONITOREO
  
CREATE TABLE dbo.Estacion_Monitoreo (
    id_estacion  INT IDENTITY(1,1) NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    ubicacion    VARCHAR(200) NOT NULL,
    CONSTRAINT PK_Estacion_Monitoreo PRIMARY KEY (id_estacion),
    CONSTRAINT UQ_Estacion_nombre UNIQUE (nombre)
);
GO

 -- REGISTRO_HIDRICO

CREATE TABLE dbo.Registro_Hidrico (
    id_registro  INT IDENTITY(1,1) NOT NULL,
    fecha_hora   DATETIME      NOT NULL CONSTRAINT DF_Registro_fecha DEFAULT (GETDATE()),
    ph           DECIMAL(3,1)  NOT NULL,       -- escala 0 a 14
    caudal       DECIMAL(5,2)  NOT NULL,       -- m3/s
    turbidez     DECIMAL(6,2)  NOT NULL,       -- NTU
    id_estacion  INT           NOT NULL,
    id_usuario   INT           NOT NULL,
    CONSTRAINT PK_Registro_Hidrico PRIMARY KEY (id_registro),
    CONSTRAINT CK_Registro_ph       CHECK (ph BETWEEN 0 AND 14),
    CONSTRAINT CK_Registro_caudal   CHECK (caudal >= 0),
    CONSTRAINT CK_Registro_turbidez CHECK (turbidez >= 0),
    CONSTRAINT FK_Registro_Estacion FOREIGN KEY (id_estacion)
        REFERENCES dbo.Estacion_Monitoreo (id_estacion) ON DELETE NO ACTION,
    CONSTRAINT FK_Registro_Usuario FOREIGN KEY (id_usuario)
        REFERENCES dbo.Usuario (id_usuario) ON DELETE NO ACTION
);
GO


   --PARAMETRO_LIMITE 

CREATE TABLE dbo.Parametro_Limite (
    id_parametro      INT IDENTITY(1,1) NOT NULL,
    nombre_parametro  VARCHAR(60)   NOT NULL,
    valor_min         DECIMAL(8,2)  NOT NULL,
    valor_max         DECIMAL(8,2)  NOT NULL,
    CONSTRAINT PK_Parametro_Limite PRIMARY KEY (id_parametro),
    CONSTRAINT UQ_Parametro_nombre UNIQUE (nombre_parametro),
    CONSTRAINT CK_Parametro_rango CHECK (valor_min <= valor_max)
);
GO

   --ALERTA

CREATE TABLE dbo.Alerta (
    id_alerta     INT IDENTITY(1,1) NOT NULL,
    fecha_hora    DATETIME    NOT NULL CONSTRAINT DF_Alerta_fecha DEFAULT (GETDATE()),
    nivel         VARCHAR(10) NOT NULL,        -- 'ALTA' o 'MEDIA'
    id_registro   INT         NOT NULL,
    id_parametro  INT         NOT NULL,
    CONSTRAINT PK_Alerta PRIMARY KEY (id_alerta),
    CONSTRAINT CK_Alerta_nivel CHECK (nivel IN ('ALTA', 'MEDIA')),
    CONSTRAINT FK_Alerta_Registro FOREIGN KEY (id_registro)
        REFERENCES dbo.Registro_Hidrico (id_registro) ON DELETE NO ACTION,
    CONSTRAINT FK_Alerta_Parametro FOREIGN KEY (id_parametro)
        REFERENCES dbo.Parametro_Limite (id_parametro) ON DELETE NO ACTION
);
GO

   --REPORTE_AMBIENTAL 
 
CREATE TABLE dbo.Reporte_Ambiental (
    id_reporte       INT IDENTITY(1,1) NOT NULL,
    fecha_generacion DATETIME     NOT NULL CONSTRAINT DF_Reporte_fecha DEFAULT (GETDATE()),
    archivo_pdf      VARCHAR(255) NOT NULL,
    id_usuario       INT          NOT NULL,
    CONSTRAINT PK_Reporte_Ambiental PRIMARY KEY (id_reporte),
    CONSTRAINT FK_Reporte_Usuario FOREIGN KEY (id_usuario)
        REFERENCES dbo.Usuario (id_usuario) ON DELETE NO ACTION
);
GO


--ZONA_AFECTACION 
   
CREATE TABLE dbo.Zona_Afectacion (
    id_zona      INT IDENTITY(1,1) NOT NULL,
    nombre_zona  VARCHAR(100) NOT NULL,
    id_estacion  INT          NOT NULL,
    CONSTRAINT PK_Zona_Afectacion PRIMARY KEY (id_zona),
    CONSTRAINT FK_Zona_Estacion FOREIGN KEY (id_estacion)
        REFERENCES dbo.Estacion_Monitoreo (id_estacion) ON DELETE NO ACTION
);
GO


   --CONVENIO_HIDRICO  

CREATE TABLE dbo.Convenio_Hidrico (
    id_convenio  INT IDENTITY(1,1) NOT NULL,
    entidad      VARCHAR(100) NOT NULL,
    fecha_firma  DATE         NOT NULL,
    id_zona      INT          NOT NULL,
    CONSTRAINT PK_Convenio_Hidrico PRIMARY KEY (id_convenio),
    CONSTRAINT FK_Convenio_Zona FOREIGN KEY (id_zona)
        REFERENCES dbo.Zona_Afectacion (id_zona) ON DELETE NO ACTION
);
GO


   --ÍNDICES 
   
CREATE INDEX IX_Usuario_rol          ON dbo.Usuario (id_rol);
CREATE INDEX IX_Registro_estacion_fh ON dbo.Registro_Hidrico (id_estacion, fecha_hora);
CREATE INDEX IX_Registro_usuario     ON dbo.Registro_Hidrico (id_usuario);
CREATE INDEX IX_Alerta_nivel_fecha   ON dbo.Alerta (nivel, fecha_hora);
CREATE INDEX IX_Alerta_registro      ON dbo.Alerta (id_registro);
CREATE INDEX IX_Alerta_parametro     ON dbo.Alerta (id_parametro);
CREATE INDEX IX_Reporte_usuario      ON dbo.Reporte_Ambiental (id_usuario);
CREATE INDEX IX_Zona_estacion        ON dbo.Zona_Afectacion (id_estacion);
CREATE INDEX IX_Convenio_zona        ON dbo.Convenio_Hidrico (id_zona);
GO


   --TRIGGER

CREATE TRIGGER dbo.TR_Registro_GenerarAlerta
ON dbo.Registro_Hidrico
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.Alerta (nivel, id_registro, id_parametro)
    SELECT
        CASE WHEN p.nombre_parametro = 'Caudal' THEN 'ALTA' ELSE 'MEDIA' END,
        i.id_registro,
        p.id_parametro
    FROM inserted i
    JOIN dbo.Parametro_Limite p
      ON (p.nombre_parametro = 'pH'       AND (i.ph       < p.valor_min OR i.ph       > p.valor_max))
      OR (p.nombre_parametro = 'Caudal'   AND (i.caudal   < p.valor_min OR i.caudal   > p.valor_max))
      OR (p.nombre_parametro = 'Turbidez' AND (i.turbidez < p.valor_min OR i.turbidez > p.valor_max));
END;
GO



  --DATOS DE PRUEBA
 

INSERT INTO dbo.Rol (nombre_rol, descripcion) VALUES
('SUPERVISOR', 'Supervisor de planta: registra parámetros hídricos'),
('ADMINISTRADOR', 'Gestiona usuarios, roles y umbrales'),
('GERENTE', 'Gerente de Medio Ambiente: dashboard y reportes'),
('AUDITOR', 'Auditor ANA/OEFA: acceso de solo lectura');

INSERT INTO dbo.Usuario (nombre, correo, password_hash, id_rol) VALUES
('Juan Pérez',  'jperez@smcv.pe',  '$2a$10$hashDeEjemploBCrypt0000000000000000000000000000000001', 1),
('Admin SMCV',  'admin@smcv.pe',   '$2a$10$hashDeEjemploBCrypt0000000000000000000000000000000002', 2),
('María Rojas', 'mrojas@smcv.pe',  '$2a$10$hashDeEjemploBCrypt0000000000000000000000000000000003', 3),
('Auditor ANA', 'auditor@ana.pe',  '$2a$10$hashDeEjemploBCrypt0000000000000000000000000000000004', 4);

INSERT INTO dbo.Estacion_Monitoreo (nombre, ubicacion) VALUES
('P01 - Bocatoma',        'Río Chili - Bocatoma'),
('P02 - Río Principal',   'Río Chili - Tramo principal'),
('P03 - Salida PTAR',     'PTAR La Enlozada - Salida'),
('P04 - Canal derivación','Canal de derivación'),
('P05 - Punto de reúso',  'Planta de molienda - Punto de reúso');

INSERT INTO dbo.Parametro_Limite (nombre_parametro, valor_min, valor_max) VALUES
('pH',       6.00,  9.00),
('Caudal',   0.00,  1.50),   -- límite del convenio: 1.5 m3/s
('Turbidez', 0.00, 25.00);

INSERT INTO dbo.Zona_Afectacion (nombre_zona, id_estacion) VALUES
('Pueblo Nuevo', 2), ('San Miguel', 2), ('El Molino', 3), ('La Esperanza', 3);

INSERT INTO dbo.Convenio_Hidrico (entidad, fecha_firma, id_zona) VALUES
('Sedapar', '2025-12-15', 1),
('Sedapar', '2025-12-15', 3);

-- Registros de prueba (el trigger genera alertas cuando hay valores fuera de umbral)
INSERT INTO dbo.Registro_Hidrico (fecha_hora, ph, caudal, turbidez, id_estacion, id_usuario) VALUES
('2026-08-26 10:30', 7.2, 1.50, 32.00, 2, 1),  -- turbidez alta -> alerta
('2026-08-26 10:00', 7.1, 1.40,  8.00, 1, 1),  -- normal
('2026-08-26 09:15', 5.6, 1.30, 15.00, 3, 1),  -- pH fuera de rango -> alerta
('2026-08-25 22:05', 7.0, 1.58, 12.00, 1, 1);  -- caudal excede convenio -> alerta ALTA

INSERT INTO dbo.Reporte_Ambiental (fecha_generacion, archivo_pdf, id_usuario) VALUES
('2026-08-25 09:00', 'RA-2026-0041.pdf', 3);
GO


   -- CONSULTAS DE VERIFICACIÓN
 
-- Alertas generadas con detalle
SELECT a.id_alerta, a.fecha_hora, a.nivel, p.nombre_parametro,
       r.ph, r.caudal, r.turbidez, e.nombre AS estacion
FROM dbo.Alerta a
JOIN dbo.Parametro_Limite   p ON p.id_parametro = a.id_parametro
JOIN dbo.Registro_Hidrico   r ON r.id_registro  = a.id_registro
JOIN dbo.Estacion_Monitoreo e ON e.id_estacion  = r.id_estacion
ORDER BY a.fecha_hora DESC;
GO
