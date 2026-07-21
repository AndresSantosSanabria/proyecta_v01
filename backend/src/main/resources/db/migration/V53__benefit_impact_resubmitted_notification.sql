INSERT INTO proyecta_db.notification_event_catalog
    (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
    ('PROJECT_BENEFIT_IMPACT_RESUBMITTED', 'Beneficio e impacto reenviado',
     'Se dispara cuando el Director corrige y reenvia la informacion de beneficio e impacto despues de una observacion.',
     'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template
    (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
    ('PROJECT_BENEFIT_IMPACT_RESUBMITTED', TRUE, FALSE, 'SUCCESS', 'GLOBAL',
     'Beneficio e impacto reenviado en {{projectId}}',
     'La informacion de beneficio e impacto del proyecto {{projectName}} fue reenviada por {{actorUsername}} tras una observacion. Revise nuevamente en {{reviewUrl}}.',
     'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
