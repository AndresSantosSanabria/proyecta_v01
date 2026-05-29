ALTER TABLE proyecta_db.tipo_documento_config
    ADD COLUMN IF NOT EXISTS require_pdf BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE proyecta_db.tipo_documento_config
    ADD COLUMN IF NOT EXISTS orden INTEGER NOT NULL DEFAULT 0;

UPDATE proyecta_db.tipo_documento_config
SET require_pdf = COALESCE(require_pdf, requiere_evidencia, TRUE);

UPDATE proyecta_db.tipo_documento_config
SET orden = CASE codigo
    WHEN 'VIABILIZACION' THEN 1
    WHEN 'ACTA_CONSTITUCION' THEN 2
    WHEN 'CRONOGRAMA' THEN 3
    WHEN 'PLAN_COMUNICACIONES' THEN 4
    ELSE COALESCE(orden, 0)
END;
