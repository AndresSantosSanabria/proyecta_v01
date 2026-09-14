-- V101: Add tipo_riesgo column to riesgos table to distinguish general vs security risks
ALTER TABLE proyecta_db.riesgos
    ADD COLUMN tipo_riesgo VARCHAR(20) NOT NULL DEFAULT 'GENERAL';
