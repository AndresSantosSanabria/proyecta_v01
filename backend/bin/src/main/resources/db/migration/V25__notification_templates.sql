CREATE TABLE IF NOT EXISTS proyecta_db.notification_event_catalog (
    code VARCHAR(120) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(40) NOT NULL,
    default_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_project_context BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.notification_template (
    event_code VARCHAR(120) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    html_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    severity VARCHAR(30) DEFAULT 'INFO',
    scope VARCHAR(30) DEFAULT 'GLOBAL',
    subject_template VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL,
    updated_by VARCHAR(120),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.notification_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES proyecta_db.usuarios(id),
    event_code VARCHAR(120) NOT NULL,
    project_id VARCHAR(30),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_notification_preference_user_event_scope UNIQUE (user_id, event_code, project_id)
);

INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES
('PROJECT_INITIAL_REGISTERED', 'Proyecto registrado inicialmente', 'Se dispara cuando un proyecto entra en estado pendiente de completitud.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_INITIAL_COMPLETED', 'Información inicial completada', 'Se dispara cuando el director completa la información inicial.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_UPDATED', 'Proyecto actualizado', 'Avisa sobre cambios relevantes en datos del proyecto.', 'BUSINESS', FALSE, TRUE, TRUE),
('PROJECT_DELAYED', 'Proyecto con retrasos', 'Avisa cuando un proyecto presenta entregables atrasados.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_CLOSED', 'Proyecto cerrado', 'Se dispara cuando el proyecto alcanza cierre formal.', 'BUSINESS', TRUE, TRUE, TRUE),
('PROJECT_ASSIGNMENT_CREATED', 'Usuario asignado al proyecto', 'Avisa cuando un usuario queda asignado al proyecto.', 'SECURITY', TRUE, TRUE, TRUE),
('DELIVERABLE_EVIDENCE_UPLOADED', 'Evidencia cargada', 'Avisa cuando se sube evidencia documental.', 'BUSINESS', FALSE, TRUE, TRUE),
('DELIVERABLE_APPROVED', 'Entregable aprobado', 'Avisa sobre la aprobación de un entregable.', 'BUSINESS', TRUE, TRUE, TRUE),
('DELIVERABLE_REJECTED', 'Entregable rechazado', 'Avisa sobre el rechazo de un entregable.', 'BUSINESS', TRUE, TRUE, TRUE),
('OBSERVATION_SUBSANATED', 'Observación subsanada', 'Avisa cuando una observación pasa a subsanada.', 'BUSINESS', TRUE, TRUE, TRUE),
('RISK_CREATED', 'Riesgo creado', 'Avisa cuando se registra un riesgo nuevo.', 'BUSINESS', FALSE, TRUE, TRUE),
('RISK_UPDATED', 'Riesgo actualizado', 'Avisa cuando se modifica un riesgo existente.', 'BUSINESS', FALSE, TRUE, TRUE),
('RISK_TREATED', 'Riesgo tratado', 'Avisa cuando un riesgo pasa a tratado.', 'BUSINESS', FALSE, TRUE, TRUE),
('SECURITY_USER_UPDATED', 'Usuario actualizado', 'Avisa cambios administrativos de usuario.', 'SECURITY', TRUE, TRUE, FALSE),
('SECURITY_ROLE_UPDATED', 'Rol actualizado', 'Avisa cambios administrativos de roles.', 'SECURITY', TRUE, TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES
('PROJECT_INITIAL_REGISTERED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Proyecto {{projectId}} registrado', 'El proyecto {{projectName}} fue registrado por {{actorUsername}}. Estado: {{state}}.', 'system', CURRENT_TIMESTAMP),
('PROJECT_INITIAL_COMPLETED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Proyecto {{projectId}} completado', 'La informacion inicial del proyecto {{projectName}} fue completada por {{actorUsername}}.', 'system', CURRENT_TIMESTAMP),
('PROJECT_UPDATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Proyecto {{projectId}} actualizado', 'Se actualizaron datos relevantes del proyecto {{projectName}}.', 'system', CURRENT_TIMESTAMP),
('PROJECT_CLOSED', TRUE, FALSE, 'ALERT', 'GLOBAL', 'Proyecto {{projectId}} cerrado', 'El proyecto {{projectName}} ha sido cerrado exitosamente.', 'system', CURRENT_TIMESTAMP),
('PROJECT_ASSIGNMENT_CREATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Asignacion de proyecto {{projectId}}', 'Se asigno el cargo {{assignmentRole}} al usuario {{assignedUsername}}.', 'system', CURRENT_TIMESTAMP),
('DELIVERABLE_EVIDENCE_UPLOADED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Evidencia cargada en {{projectId}}', 'El entregable {{deliverableName}} recibio una nueva evidencia.', 'system', CURRENT_TIMESTAMP),
('DELIVERABLE_APPROVED', TRUE, FALSE, 'SUCCESS', 'GLOBAL', 'Entregable aprobado en {{projectId}}', 'El entregable {{deliverableName}} fue aprobado.', 'system', CURRENT_TIMESTAMP),
('DELIVERABLE_REJECTED', TRUE, FALSE, 'WARNING', 'GLOBAL', 'Entregable rechazado en {{projectId}}', 'El entregable {{deliverableName}} fue rechazado. Observacion: {{observation}}.', 'system', CURRENT_TIMESTAMP),
('OBSERVATION_SUBSANATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Observacion subsanada en {{projectId}}', 'La observacion {{observationId}} quedo subsanada.', 'system', CURRENT_TIMESTAMP),
('RISK_CREATED', TRUE, FALSE, 'WARNING', 'GLOBAL', 'Riesgo creado en {{projectId}}', 'Se creo el riesgo {{riskCode}} con nivel {{riskLevel}}.', 'system', CURRENT_TIMESTAMP),
('RISK_UPDATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Riesgo actualizado en {{projectId}}', 'Se actualizo el riesgo {{riskCode}}.', 'system', CURRENT_TIMESTAMP),
('RISK_TREATED', TRUE, FALSE, 'SUCCESS', 'GLOBAL', 'Riesgo tratado en {{projectId}}', 'El riesgo {{riskCode}} paso a estado tratado.', 'system', CURRENT_TIMESTAMP),
('SECURITY_USER_UPDATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Usuario actualizado', 'Se actualizaron datos de seguridad para {{username}}.', 'system', CURRENT_TIMESTAMP),
('SECURITY_ROLE_UPDATED', TRUE, FALSE, 'INFO', 'GLOBAL', 'Rol actualizado', 'Se modifico el rol {{roleCode}}.', 'system', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO NOTHING;
