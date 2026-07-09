INSERT INTO proyecta_db.notification_event_catalog
    (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
    ('CLOSURE_APPROVED', 'Cierre de proyecto aprobado',
     'Se dispara cuando el Gestor o Administrador aprueba la solicitud de cierre del Director.',
     'BUSINESS', TRUE, TRUE, TRUE),
    ('CLOSURE_REJECTED', 'Cierre de proyecto rechazado',
     'Se dispara cuando el Gestor o Administrador rechaza la solicitud de cierre del Director.',
     'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template
    (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
    ('CLOSURE_APPROVED', TRUE, FALSE, 'INFO', 'GLOBAL',
     'Cierre aprobado en proyecto {{projectName}}',
     'Tu solicitud de cierre para el proyecto {{projectName}} fue aprobada por {{approver}}. El proyecto estara formalmente cerrado.',
     'system', CURRENT_TIMESTAMP),
    ('CLOSURE_REJECTED', TRUE, FALSE, 'WARNING', 'GLOBAL',
     'Cierre rechazado en proyecto {{projectName}}',
     'Tu solicitud de cierre para el proyecto {{projectName}} fue rechazada por {{rejector}}. Motivo: {{observaciones}}. Por favor, corrige las observaciones y vuelve a enviar la solicitud.',
     'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
