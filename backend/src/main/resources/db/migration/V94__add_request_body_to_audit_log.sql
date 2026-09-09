-- ========================================================
-- Agrega la columna request_body a la tabla system_audit_log
-- para almacenar el payload enviado en requests POST/PUT/PATCH.
-- ========================================================

ALTER TABLE proyecta_db.system_audit_log
ADD COLUMN IF NOT EXISTS request_body TEXT;