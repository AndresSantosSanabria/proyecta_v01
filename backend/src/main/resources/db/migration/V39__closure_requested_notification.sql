INSERT INTO proyecta_db.notification_event_catalog
    (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
    ('CLOSURE_REQUESTED', 'Cierre de proyecto solicitado',
     'Se dispara cuando el Director solicita formalmente el cierre del proyecto al Gestor.',
     'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template
    (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
    ('CLOSURE_REQUESTED', TRUE, FALSE, 'WARNING', 'GLOBAL',
     'Solicitud de cierre en proyecto {{projectName}}',
     'El usuario {{requester}} solicito formalmente el cierre del proyecto {{projectName}}. Por favor, proceda con las validaciones y el cierre correspondiente.',
     'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
