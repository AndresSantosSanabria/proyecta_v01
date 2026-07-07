INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
('PROJECT_BENEFIT_IMPACT_REVIEWED', 'Beneficio e impacto revisado', 'Se dispara cuando el gestor aprueba o rechaza la informacion de beneficio e impacto.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('PROJECT_BENEFIT_IMPACT_REVIEWED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Beneficio e impacto {{aprobado}} en {{projectId}}', 'La informacion de beneficio e impacto del proyecto {{projectName}} fue {{aprobado}} por el gestor. Observaciones: {{observaciones}}', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
