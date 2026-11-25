-- Limita intentos del código temporal de recuperación sin eliminar tokens ni usuarios.
ALTER TABLE password_reset_token
    ADD COLUMN IF NOT EXISTS intentos INTEGER NOT NULL DEFAULT 0;
