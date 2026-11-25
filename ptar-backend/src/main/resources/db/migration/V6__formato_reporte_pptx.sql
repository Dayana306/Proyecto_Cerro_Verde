-- Amplía los formatos oficiales de reporte para admitir presentaciones PowerPoint.
ALTER TABLE reporte
    ADD CONSTRAINT ck_reporte_formato
    CHECK (formato IN ('PDF', 'XLSX', 'PPTX'));
