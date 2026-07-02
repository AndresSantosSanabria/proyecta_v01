ALTER TABLE proyecta_db.tipo_documento_config
    ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW();
