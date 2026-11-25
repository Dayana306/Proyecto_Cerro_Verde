-- Permite publicar comunicados con prioridad BAJA sin modificar ni eliminar registros existentes.
ALTER TABLE alerta_publicada
    DROP CONSTRAINT IF EXISTS ck_alerta_publicada_nivel;

ALTER TABLE alerta_publicada
    ADD CONSTRAINT ck_alerta_publicada_nivel
    CHECK (nivel IN ('BAJA', 'MEDIA', 'ALTA'));
