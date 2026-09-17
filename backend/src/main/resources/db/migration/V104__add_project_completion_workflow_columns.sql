-- Quality Gate: Documentos Pre-Wizard y flujo de completitud
-- Adds viabilidad tracking, document verification, and forced closure columns

ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS viabilidad_estado VARCHAR(20) DEFAULT 'PENDIENTE';
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS viabilidad_observaciones TEXT;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS viabilidad_revisado_por VARCHAR(120);
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS viabilidad_revisado_en TIMESTAMP;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS documentos_cargados BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS documentos_verificados BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS fecha_verificacion_documentos TIMESTAMP;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS fecha_limite_completar DATE;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS cierre_forzoso BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS cierre_forzoso_por VARCHAR(120);
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS cierre_forzoso_en TIMESTAMP;
ALTER TABLE proyecto ADD COLUMN IF NOT EXISTS acta_constitucion_cargada BOOLEAN NOT NULL DEFAULT FALSE;
