-- ============================================================================
-- V89: Notificacion de alerta al director desde avance de proyecto
-- Objetivo: Registrar el evento PROJECT_DIRECTOR_ALERT para que el gestor
--           o admin pueda enviar un resumen del estado del proyecto al
--           director asignado via correo electronico e notificacion in-app.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Registrar el evento en el catalogo
-- ---------------------------------------------------------------------------
INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES ('PROJECT_DIRECTOR_ALERT', 'Alerta de avance al director', 'Notificacion manual enviada por el gestor/admin al director del proyecto con el resumen de avance, riesgos y entregables pendientes.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Crear el template de notificacion (configurable desde Admin)
-- ---------------------------------------------------------------------------
INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, target_roles, updated_by, updated_at)
VALUES (
    'PROJECT_DIRECTOR_ALERT',
    TRUE,
    FALSE,
    'WARNING',
    'GLOBAL',
    'Seguimiento de avance: {{projectName}} ({{projectId}})',
    'Estimado(a) {{directorName}},\n\nLe enviamos este recordatorio sobre el estado actual del proyecto asignado.\n\n--- INFORMACION DEL PROYECTO ---\n  Codigo: {{projectId}}\n  Nombre: {{projectName}}\n  Estado actual: {{projectState}}\n  Avance total: {{avanceTotal}}%%\n\n--- RIESGOS PENDIENTES ({{pendingRisksCount}}) ---\n{{pendingRisksDetail}}\n--- ENTREGABLES PENDIENTES POR CARGAR EVIDENCIA ({{pendingDeliverablesCount}}) ---\n{{pendingDeliverablesDetail}}\n{{#hasOverdue}}\n--- ENTREGABLES VENCIDOS ({{overdueCount}}) ---\n  ATENCION: Los siguientes entregables han superado su fecha limite:\n{{overdueDetail}}\n{{/hasOverdue}}\n{{#hasMissingDocuments}}\n--- DOCUMENTOS DEL PROYECTO PENDIENTES POR CARGAR ---\n  Los siguientes documentos requeridos no han sido cargados:\n{{missingDocumentsDetail}}\n{{/hasMissingDocuments}}\nPuede revisar el avance completo del proyecto en la plataforma:\n{{projectUrl}}\n\n---\nEste mensaje fue enviado por {{sentBy}} el {{sentAt}}.\nPuede administrar sus preferencias de notificacion en la plataforma.',
    NULL,
    'flyway',
    NOW()
)
ON CONFLICT (event_code) DO NOTHING;
