INSERT INTO proyecta_db.notification_template (
    event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at
)
VALUES
('SECURITY_USER_UPDATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Usuario actualizado', 'Se actualizaron datos de seguridad para {{username}}.', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
