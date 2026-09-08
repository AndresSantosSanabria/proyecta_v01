-- ============================================================================
-- V90: Correccion del template de alerta al director
-- Corrige: newlines literales (\n), secciones condicionales invalidas
--          para el renderer, y doble percent (%%).
-- Usa chr(10) para saltos de linea ya que PostgreSQL no permite mezclar
-- E'...' con '...' en concatenacion implicita.
-- ============================================================================

UPDATE proyecta_db.notification_template
SET subject_template = 'Seguimiento de avance: {{projectName}} ({{projectId}})',
    body_template    = 'Estimado(a) {{directorName}},'
        || chr(10) || chr(10)
        || 'Le enviamos este recordatorio sobre el estado actual del proyecto asignado.'
        || chr(10) || chr(10)
        || '--- INFORMACION DEL PROYECTO ---'
        || chr(10)
        || '  Codigo: {{projectId}}'
        || chr(10)
        || '  Nombre: {{projectName}}'
        || chr(10)
        || '  Estado actual: {{projectState}}'
        || chr(10)
        || '  Avance total: {{avanceTotal}}%'
        || chr(10) || chr(10)
        || '--- RIESGOS PENDIENTES ({{pendingRisksCount}}) ---'
        || chr(10)
        || '{{pendingRisksDetail}}'
        || chr(10)
        || '--- ENTREGABLES PENDIENTES POR CARGAR EVIDENCIA ({{pendingDeliverablesCount}}) ---'
        || chr(10)
        || '{{pendingDeliverablesDetail}}'
        || chr(10)
        || '--- ENTREGABLES VENCIDOS ({{overdueCount}}) ---'
        || chr(10)
        || '  ATENCION: Los siguientes entregables han superado su fecha limite:'
        || chr(10)
        || '{{overdueDetail}}'
        || chr(10)
        || '--- DOCUMENTOS DEL PROYECTO PENDIENTES POR CARGAR ---'
        || chr(10)
        || '  Los siguientes documentos requeridos no han sido cargados:'
        || chr(10)
        || '{{missingDocumentsDetail}}'
        || chr(10) || chr(10)
        || 'Puede revisar el avance completo del proyecto en la plataforma:'
        || chr(10)
        || '{{projectUrl}}'
        || chr(10) || chr(10)
        || '---'
        || chr(10)
        || 'Este mensaje fue enviado por {{sentBy}} el {{sentAt}}.'
        || chr(10)
        || 'Puede administrar sus preferencias de notificacion en la plataforma.',
    severity         = 'WARNING',
    html_enabled     = FALSE,
    updated_by       = 'flyway',
    updated_at       = NOW()
WHERE event_code = 'PROJECT_DIRECTOR_ALERT';
