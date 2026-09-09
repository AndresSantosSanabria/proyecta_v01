-- ========================================================
-- Enhance system_audit_log with entity tracking & response body
-- Adds: entidad_tipo, entidad_id, respuesta_body for full
-- before/after audit trail capability.
-- ========================================================

ALTER TABLE proyecta_db.system_audit_log
ADD COLUMN IF NOT EXISTS entidad_tipo VARCHAR(100);

ALTER TABLE proyecta_db.system_audit_log
ADD COLUMN IF NOT EXISTS entidad_id VARCHAR(100);

ALTER TABLE proyecta_db.system_audit_log
ADD COLUMN IF NOT EXISTS respuesta_body TEXT;

CREATE INDEX IF NOT EXISTS idx_audit_log_entidad_tipo ON proyecta_db.system_audit_log (entidad_tipo);
CREATE INDEX IF NOT EXISTS idx_audit_log_entidad_id ON proyecta_db.system_audit_log (entidad_id);
