-- ============================================================================
-- V80: Mejora de textos de notificaciones
-- Objetivo: Hacer las notificaciones mas informativas y comprensibles.
--           Cada notificacion debe indicar clarmente: QUE proyecto, QUE
--           entregable/riesgo/accion, y POR QUE se esta notificando.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. PROYECTO REGISTRADO INICIALMENTE
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Nuevo proyecto registrado: {{projectName}}',
    body_template    = 'El proyecto "{{projectName}}" ({{projectId}}) fue registrado por {{actorUsername}} y se encuentra en estado "{{state}}". Dirija el proyecto para que el director pueda completar la informacion inicial.'
WHERE event_code = 'PROJECT_INITIAL_REGISTERED';

-- ---------------------------------------------------------------------------
-- 2. INFORMACION INICIAL COMPLETADA
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Informacion inicial completada: {{projectName}}',
    body_template    = 'El director {{actorUsername}} completo la informacion inicial del proyecto "{{projectName}}" ({{projectId}}). El proyecto esta ahora en estado "{{state}}" y puede avanzar en su ejecucion.'
WHERE event_code = 'PROJECT_INITIAL_COMPLETED';

-- ---------------------------------------------------------------------------
-- 3. PROYECTO ACTUALIZADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Proyecto actualizado: {{projectName}}',
    body_template    = 'Se actualizaron datos del proyecto "{{projectName}}" ({{projectId}}) por {{actorUsername}}. Revise los cambios en la plataforma para verificar que la informacion este al dia.'
WHERE event_code = 'PROJECT_UPDATED';

-- ---------------------------------------------------------------------------
-- 4. PROYECTO CON RETRASOS
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Proyecto con retrasos: {{projectName}}',
    body_template    = 'El proyecto "{{projectName}}" ({{projectId}}) presenta {{overdueDeliverables}} entregable(s) con fecha de entrega vencida. Es necesario revisar el avance y tomar acciones para recuperar el cronograma.'
WHERE event_code = 'PROJECT_DELAYED';

-- ---------------------------------------------------------------------------
-- 5. PROYECTO CERRADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Proyecto cerrado: {{projectName}}',
    body_template    = 'El proyecto "{{projectName}}" ({{projectId}}) ha sido cerrado exitosamente. El proyecto paso al estado "{{state}}" y ya no acepta modificaciones.'
WHERE event_code = 'PROJECT_CLOSED';

-- ---------------------------------------------------------------------------
-- 6. USUARIO ASIGNADO AL PROYECTO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Nueva asignacion en proyecto: {{projectName}}',
    body_template    = 'El usuario {{assignedUsername}} fue asignado al proyecto "{{projectName}}" ({{projectId}}) con el cargo de "{{assignmentRole}}". Ya puede acceder a la informacion del proyecto en la plataforma.'
WHERE event_code = 'PROJECT_ASSIGNMENT_CREATED';

-- ---------------------------------------------------------------------------
-- 7. DOCUMENTO CARGADO EN PROYECTO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Documento cargado: {{documentType}} - {{projectName}}',
    body_template    = 'Se cargo el documento "{{documentType}}" ({{documentName}}) en el proyecto "{{projectName}}" ({{projectId}}) por {{actorUsername}}. Revise el documento en la seccion de documentos del proyecto.'
WHERE event_code = 'PROJECT_DOCUMENT_UPLOADED';

-- ---------------------------------------------------------------------------
-- 8. DOCUMENTO ELIMINADO EN PROYECTO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Documento eliminado: {{documentType}} - {{projectName}}',
    body_template    = 'El documento "{{documentType}}" ({{documentName}}) del proyecto "{{projectName}}" ({{projectId}}) fue eliminado por {{actorUsername}}. Verifique si es necesario restaurar o reemplazar el documento.'
WHERE event_code = 'PROJECT_DOCUMENT_DELETED';

-- ---------------------------------------------------------------------------
-- 9. EVIDENCIA CARGADA EN ENTREGABLE
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Evidencia cargada: {{deliverableName}} - {{projectName}}',
    body_template    = 'Se cargo una nueva evidencia en el entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}). Revise la evidencia adjunta en la seccion de avance del entregable.'
WHERE event_code = 'DELIVERABLE_EVIDENCE_UPLOADED';

-- ---------------------------------------------------------------------------
-- 10. ENTREGABLE APROBADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Entregable aprobado: {{deliverableName}} - {{projectName}}',
    body_template    = 'El entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue aprobado por {{actorUsername}}. El avance del proyecto se ha actualizado automaticamente.'
WHERE event_code = 'DELIVERABLE_APPROVED';

-- ---------------------------------------------------------------------------
-- 11. ENTREGABLE RECHAZADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Entregable rechazado: {{deliverableName}} - {{projectName}}',
    body_template    = 'El entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue rechazado por {{actorUsername}}. Observacion: "{{observation}}". Por favor, subsane la observacion y vuelva a enviar la evidencia.'
WHERE event_code = 'DELIVERABLE_REJECTED';

-- ---------------------------------------------------------------------------
-- 12. OBSERVACION SUBSANADA
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Observacion subsanada: {{deliverableName}} - {{projectName}}',
    body_template    = 'La observacion #{{observationId}} del entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue subsanada por {{actorUsername}}. La evidencia ha sido corregida y esta lista para nueva revision.'
WHERE event_code = 'OBSERVATION_SUBSANATED';

-- ---------------------------------------------------------------------------
-- 13. BENEFICIO E IMPACTO REQUERIDO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Beneficio e impacto requerido: {{projectName}}',
    body_template    = 'El proyecto "{{projectName}}" ({{projectId}}) alcanzo el 100%% de entregables aprobados. El Director debe diligenciar la informacion de beneficio e impacto para continuar con el proceso de cierre. Acceda desde: {{benefitImpactUrl}}.'
WHERE event_code = 'PROJECT_BENEFIT_IMPACT_REQUIRED';

-- ---------------------------------------------------------------------------
-- 14. BENEFICIO E IMPACTO DILIGENCIADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Beneficio e impacto diligenciado: {{projectName}}',
    body_template    = 'El director {{actorUsername}} registro la informacion de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}). La informacion esta disponible para revision del gestor en: {{reviewUrl}}.'
WHERE event_code = 'PROJECT_BENEFIT_IMPACT_SUBMITTED';

-- ---------------------------------------------------------------------------
-- 15. BENEFICIO E IMPACTO REENVIADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Beneficio e impacto corregido: {{projectName}}',
    body_template    = 'El director {{actorUsername}} corrigio y reenvio la informacion de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}) despues de una observacion. Revise la informacion corregida en: {{reviewUrl}}.'
WHERE event_code = 'PROJECT_BENEFIT_IMPACT_RESUBMITTED';

-- ---------------------------------------------------------------------------
-- 16. BENEFICIO E IMPACTO REVISADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Beneficio e impacto {{aprobado}}: {{projectName}}',
    body_template    = 'La informacion de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}) fue {{aprobado}} por el gestor. {{observaciones}}'
WHERE event_code = 'PROJECT_BENEFIT_IMPACT_REVIEWED';

-- ---------------------------------------------------------------------------
-- 17. RIESGO CREADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Riesgo creado: {{riskCode}} - {{projectName}}',
    body_template    = 'Se registro el riesgo {{riskCode}} en el proyecto "{{projectName}}" ({{projectId}}) con nivel de impacto "{{riskLevel}}". Revise los detalles del riesgo en la seccion de gestion de riesgos del proyecto.'
WHERE event_code = 'RISK_CREATED';

-- ---------------------------------------------------------------------------
-- 18. RIESGO ACTUALIZADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Riesgo actualizado: {{riskCode}} - {{projectName}}',
    body_template    = 'El riesgo {{riskCode}} del proyecto "{{projectName}}" ({{projectId}}) fue modificado por {{actorUsername}}. Verifique los cambios realizados en la seccion de gestion de riesgos.'
WHERE event_code = 'RISK_UPDATED';

-- ---------------------------------------------------------------------------
-- 19. RIESGO TRATADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Riesgo tratado: {{riskCode}} - {{projectName}}',
    body_template    = 'El riesgo {{riskCode}} del proyecto "{{projectName}}" ({{projectId}}) paso a estado "Tratado". Se aplicaron las acciones de mitigacion o tratamiento correspondientes.'
WHERE event_code = 'RISK_TREATED';

-- ---------------------------------------------------------------------------
-- 20. SOLICITUD DE CIERRE (ya era bueno, solo mejora leve)
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Solicitud de cierre: {{projectName}}',
    body_template    = 'El usuario {{requester}} solicito formalmente el cierre del proyecto "{{projectName}}" ({{projectId}}). Como gestor, proceda con las validaciones correspondientes y apruebe o rechace la solicitud.'
WHERE event_code = 'CLOSURE_REQUESTED';

-- ---------------------------------------------------------------------------
-- 21. CIERRE APROBADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Cierre aprobado: {{projectName}}',
    body_template    = 'La solicitud de cierre del proyecto "{{projectName}}" ({{projectId}}) fue aprobada por {{approver}}. El proyecto estara formalmente cerrado y no aceptara mas modificaciones.'
WHERE event_code = 'CLOSURE_APPROVED';

-- ---------------------------------------------------------------------------
-- 22. CIERRE RECHAZADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Cierre rechazado: {{projectName}}',
    body_template    = 'La solicitud de cierre del proyecto "{{projectName}}" ({{projectId}}) fue rechazada por {{rejector}}. Motivo: "{{observaciones}}". Por favor, corrija las observaciones y envie una nueva solicitud de cierre.'
WHERE event_code = 'CLOSURE_REJECTED';

-- ---------------------------------------------------------------------------
-- 23. ROL DE SEGURIDAD ACTUALIZADO
-- ---------------------------------------------------------------------------
UPDATE proyecta_db.notification_template
SET subject_template = 'Configuracion de seguridad actualizada',
    body_template    = 'Se modifico el rol "{{roleCode}}" en la configuracion de seguridad del sistema. Si tiene permisos afectados, verifique que su acceso siga funcionando correctamente.'
WHERE event_code = 'SECURITY_ROLE_UPDATED';

-- ---------------------------------------------------------------------------
-- 24. ENTREGABLE - FECHA CAMBIADA (nuevo template)
-- ---------------------------------------------------------------------------
INSERT INTO proyecta_db.notification_event_catalog (code, name, description, category, default_enabled, active, requires_project_context)
VALUES ('ENTREGABLE_FECHA_CAMBIADA', 'Entregable - fecha cambiada', 'Se dispara cuando se modifica la fecha de entrega de un entregable.', 'BUSINESS', TRUE, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO proyecta_db.notification_template (event_code, enabled, html_enabled, severity, scope, subject_template, body_template, updated_by, updated_at)
VALUES ('ENTREGABLE_FECHA_CAMBIADA', TRUE, FALSE, 'WARNING', 'GLOBAL',
    'Fecha de entrega cambiada: {{entregableNombre}}',
    'La fecha de entrega del entregable "{{entregableNombre}}" fue cambiada. Fecha anterior: {{fechaAnterior}}. Nueva fecha: {{fechaNueva}}. Justificacion: "{{justificacion}}". Verifique el impacto en el cronograma del proyecto.',
    'flyway', NOW())
ON CONFLICT (event_code) DO NOTHING;
