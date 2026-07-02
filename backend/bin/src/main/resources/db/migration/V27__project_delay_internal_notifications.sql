INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
('PROJECT_DELAYED', 'Proyecto con retrasos', 'Avisa cuando un proyecto presenta entregables atrasados.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('PROJECT_DELAYED', TRUE, FALSE, 'WARNING', 'GLOBAL', 'Proyecto {{projectId}} con retrasos', 'El proyecto {{projectName}} presenta {{overdueDeliverables}} entregables atrasados.', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
