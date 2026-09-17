ALTER TABLE proyecta_db.documento DROP CONSTRAINT IF EXISTS chk_tipo_documento;

ALTER TABLE proyecta_db.documento ADD CONSTRAINT chk_tipo_documento
    CHECK (tipo_documento IN ('VIABILIZACION', 'ACTA_CONSTITUCION', 'CRONOGRAMA', 'PLAN_COMUNICACIONES', 'MATRIZ_RIESGOS_VIABILIDAD'));
