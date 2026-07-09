ALTER TABLE proyecta_db.proyecto ADD COLUMN IF NOT EXISTS cierre_estado VARCHAR(30) NULL;
ALTER TABLE proyecta_db.proyecto ADD COLUMN IF NOT EXISTS cierre_observaciones TEXT NULL;
