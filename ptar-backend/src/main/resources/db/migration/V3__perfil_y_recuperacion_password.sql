-- Nuevas funciones: foto de perfil y restablecimiento seguro de contraseña.
-- V1/V2 se conservan inmutables para no afectar entornos que ya las aplicaron.
ALTER TABLE usuario ADD COLUMN foto_perfil LONGTEXT NULL;

CREATE TABLE password_reset_token (
  id_token BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  fecha_expiracion DATETIME NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_password_reset_token_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX idx_password_reset_usuario_estado (id_usuario, usado),
  INDEX idx_password_reset_expiracion (fecha_expiracion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
