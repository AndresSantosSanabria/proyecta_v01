ALTER TABLE riesgos
    ADD COLUMN created_by VARCHAR(150);

COMMENT ON COLUMN riesgos.created_by IS 'Usuario que creo el registro del riesgo';
