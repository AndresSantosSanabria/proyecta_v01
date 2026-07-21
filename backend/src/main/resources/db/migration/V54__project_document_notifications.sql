INSERT INTO proyecta_db.notification_event_catalog (
    code, name, description, category, default_enabled, active, requires_project_context
)
VALUES
('PROJECT_DOCUMENT_UPLOADED', 'Documento del proyecto cargado', 'Se dispara cuando un usuario carga o reemplaza un documento del proyecto.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_DOCUMENT_DELETED', 'Documento del proyecto eliminado', 'Se dispara cuando un usuario elimina un documento del proyecto.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (
    event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at
)
VALUES
('PROJECT_DOCUMENT_UPLOADED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Documento cargado en {{projectName}}', 'El documento {{documentType}} del proyecto {{projectName}} fue cargado por {{actorUsername}}.', 'system', CURRENT_TIMESTAMP),
('PROJECT_DOCUMENT_DELETED', TRUE, FALSE, 'WARNING', 'GLOBAL', 'Documento eliminado en {{projectName}}', 'El documento {{documentType}} del proyecto {{projectName}} fue eliminado por {{actorUsername}}.', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
