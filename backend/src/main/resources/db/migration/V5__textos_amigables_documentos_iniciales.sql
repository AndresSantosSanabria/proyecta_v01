-- =============================================================================
-- V5: TEXTOS AMIGABLES PARA DOCUMENTOS INICIALES DEL PROYECTO
-- =============================================================================
-- Contexto:
--   La semilla V2 nombraba los eventos de notificacion y las descripciones de
--   los parametros del sistema con la jerga tecnica "Pre-Wizard", visible en
--   el panel de plantillas de notificacion y en el panel de parametros.
--
-- Problema:
--   El usuario final (director/gestor) no comprende el termino "wizard";
--   se pide usar un lenguaje comun: "documentos iniciales del proyecto".
--
-- La migracion es idempotente: solo actualiza los textos sembrados en V2
-- (valores y descripciones ya existentes). No modifica claves, rutas ni
-- estructura.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) Catalogo de eventos de notificacion (nombres visibles en el panel)
-- -----------------------------------------------------------------------------
UPDATE proyecta_db.notification_event_catalog
   SET name = 'Documentos Iniciales Cargados',
       description = 'Notificación unificada con el estado de los 3 documentos iniciales del proyecto'
 WHERE code = 'VIABILIDAD_UPLOADED';

UPDATE proyecta_db.notification_event_catalog
   SET name = 'Documentos Iniciales Aprobados',
       description = 'Notificación unificada: documentos iniciales del proyecto aprobados'
 WHERE code = 'VIABILIDAD_APPROVED';

UPDATE proyecta_db.notification_event_catalog
   SET name = 'Documentos Iniciales Devueltos',
       description = 'Notificación unificada: documentos iniciales del proyecto devueltos'
 WHERE code = 'VIABILIDAD_RETURNED';

-- -----------------------------------------------------------------------------
-- 2) Descripciones de parametros del sistema (visibles en el panel de parametros)
-- -----------------------------------------------------------------------------
UPDATE proyecta_db.system_parameters
   SET descripcion = 'Etiqueta de estados en correos de documentos iniciales'
 WHERE param_key = 'notif_prewizard_estado_etiqueta'
   AND (descripcion IS NULL OR descripcion ILIKE '%pre-wizard%');

UPDATE proyecta_db.system_parameters
   SET descripcion = 'Texto del enlace en correos de documentos iniciales'
 WHERE param_key = 'notif_prewizard_enlace_texto'
   AND (descripcion IS NULL OR descripcion ILIKE '%pre-wizard%');
