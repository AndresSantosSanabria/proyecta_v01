-- Activa la notificacion de evidencia cargada y muestra el nombre del proyecto.
UPDATE proyecta_db.notification_template
SET
    enabled = TRUE,
    subject_template = 'Evidencia cargada en {{projectName}}',
    body_template = 'El entregable {{deliverableName}} del proyecto {{projectName}} recibio una nueva evidencia.'
WHERE event_code = 'DELIVERABLE_EVIDENCE_UPLOADED';

-- Si la plantilla no existe en algun ambiente, la creamos con el contenido correcto.
INSERT INTO proyecta_db.notification_template (
    event_code,
    enabled,
    html_enabled,
    severity,
    scope,
    subject_template,
    body_template,
    updated_by,
    updated_at
)
SELECT
    'DELIVERABLE_EVIDENCE_UPLOADED',
    TRUE,
    FALSE,
    'INFO',
    'GLOBAL',
    'Evidencia cargada en {{projectName}}',
    'El entregable {{deliverableName}} del proyecto {{projectName}} recibio una nueva evidencia.',
    'system',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM proyecta_db.notification_template
    WHERE event_code = 'DELIVERABLE_EVIDENCE_UPLOADED'
);
