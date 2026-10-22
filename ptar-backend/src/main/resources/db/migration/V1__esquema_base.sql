
--V1 PROVISIONAL: solo las tablas necesarias para autenticación y usuarios.
--base de datos trabaja aqui


CREATE TABLE rol (
  id_rol INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE,
  descripcion VARCHAR(200),
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE area (
  id_area INT AUTO_INCREMENT PRIMARY KEY,
  nombre_area VARCHAR(100) NOT NULL,
  descripcion VARCHAR(200),
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  apellido VARCHAR(80) NOT NULL,
  correo VARCHAR(120) NOT NULL UNIQUE,
  usuario_login VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  telefono VARCHAR(20),
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_rol INT NOT NULL,
  id_area INT NULL,
  CONSTRAINT fk_usuario_rol  FOREIGN KEY (id_rol)  REFERENCES rol(id_rol)   ON DELETE RESTRICT,
  CONSTRAINT fk_usuario_area FOREIGN KEY (id_area) REFERENCES area(id_area) ON DELETE RESTRICT
) ENGINE=InnoDB;
