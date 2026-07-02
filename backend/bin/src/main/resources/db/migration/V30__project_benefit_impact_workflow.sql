CREATE TABLE IF NOT EXISTS proyecta_db.proyecto_beneficio_impacto (
    id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL UNIQUE REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    estado VARCHAR(30) NOT NULL,
    requerido_en TIMESTAMP,
    requerido_por VARCHAR(120),
    diligenciado_en TIMESTAMP,
    diligenciado_por VARCHAR(120),
    revisado_en TIMESTAMP,
    revisado_por VARCHAR(120),
    poblacion_beneficiada_directa INTEGER,
    poblacion_beneficiada_indirecta INTEGER,
    poblacion_objetivo INTEGER,
    territorio_beneficiado VARCHAR(200),
    beneficio_principal TEXT,
    impacto_social TEXT,
    impacto_institucional TEXT,
    impacto_economico TEXT,
    alineacion_plan_desarrollo TEXT,
    alineacion_peti TEXT,
    metas_contribuidas TEXT,
    indicador_base TEXT,
    indicador_meta TEXT,
    indicador_resultado TEXT,
    fuente_verificacion TEXT,
    observaciones TEXT,
    snapshot_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proyecto_beneficio_impacto_estado
    ON proyecta_db.proyecto_beneficio_impacto (estado);

INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
('PROJECT_BENEFIT_IMPACT_REQUIRED', 'Beneficio e impacto requerido', 'Se dispara cuando el proyecto alcanza 100 por ciento de entregables aprobados.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_BENEFIT_IMPACT_SUBMITTED', 'Beneficio e impacto diligenciado', 'Se dispara cuando el director registra la informacion de beneficio e impacto.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('PROJECT_BENEFIT_IMPACT_REQUIRED', TRUE, FALSE, 'WARNING', 'GLOBAL', 'Proyecto {{projectId}} requiere beneficio e impacto', 'El proyecto {{projectName}} alcanzo el 100 por ciento de entregables aprobados. El Director debe diligenciar la informacion de beneficio e impacto en {{benefitImpactUrl}}.', 'system', CURRENT_TIMESTAMP),
('PROJECT_BENEFIT_IMPACT_SUBMITTED', TRUE, FALSE, 'SUCCESS', 'GLOBAL', 'Beneficio e impacto diligenciado en {{projectId}}', 'La informacion de beneficio e impacto del proyecto {{projectName}} fue diligenciada por {{actorUsername}} y ya esta disponible para revision en {{reviewUrl}}.', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
