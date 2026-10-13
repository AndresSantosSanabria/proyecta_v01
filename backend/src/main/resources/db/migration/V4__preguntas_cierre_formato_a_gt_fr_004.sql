-- =============================================================================
-- V4: BANCO DE PREGUNTAS DE CIERRE - FORMATO A-GT-FR-004 (Acta de Cierre)
-- =============================================================================
-- Contexto:
--   ProjectClosureServiceImpl.buildRequestFromSavedAnswers() arma el Acta de
--   Cierre (DOCX) a partir de las respuestas del banco de preguntas usando el
--   campo `orden` (con keyword de respaldo en el texto de la pregunta):
--     orden 1 -> Resumen ejecutivo             (keyword: "resumen")
--     orden 2 -> Aspectos positivos            (keyword: "positivo")
--     orden 3 -> Aspectos a mejorar            (keyword: "mejor")
--     orden 4 -> Recomendaciones               (keyword: "recomend")
--     orden 5 -> Transferencia de conocimiento (keyword: "transfer")
--   El resto del banco (orden >= 6) conforma el checklist complementario del
--   formato: resultados, cierre administrativo, riesgos y observaciones.
--
-- Problema:
--   La semilla V2 dejo preguntas con tipos no soportados por la UI
--   (`si_no`, `si_no_detallado` -> solo se admiten texto_libre,
--   seleccion_unica, seleccion_multiple, fecha y numero) y en un orden que no
--   corresponde al acta, por lo que el apartado "Preguntas Cierre" no
--   reflejaba el formato oficial de cierre de proyecto.
--
-- La migracion es idempotente: normaliza las preguntas semilla existentes y
-- solo inserta las preguntas que falten (comparando por texto).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) Normaliza las preguntas semilla existentes (texto/tipo/orden del formato)
-- -----------------------------------------------------------------------------

-- orden 2 -> Aspectos positivos (renombra la pregunta generica de lecciones)
UPDATE proyecta_db.project_closure_question
   SET texto = 'Describa los aspectos positivos del proyecto (qué funcionó bien).',
       tipo_respuesta = 'texto_libre',
       opciones = NULL,
       orden = 2,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿Qué lecciones aprendidas destacaría del proyecto?'
   AND NOT EXISTS (
        SELECT 1 FROM proyecta_db.project_closure_question q
         WHERE q.texto = 'Describa los aspectos positivos del proyecto (qué funcionó bien).'
   );

-- orden 4 -> Recomendaciones (texto de la semilla ya coincide con el formato)
UPDATE proyecta_db.project_closure_question
   SET tipo_respuesta = 'texto_libre',
       opciones = NULL,
       orden = 4,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿Qué recomendaciones haría para futuros proyectos similares?';

-- orden 6 -> Objetivos cumplidos (si_no -> seleccion_unica)
UPDATE proyecta_db.project_closure_question
   SET tipo_respuesta = 'seleccion_unica',
       opciones = '["Sí","No"]'::jsonb,
       orden = 6,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿Se cumplieron todos los objetivos del proyecto?';

-- orden 7 -> Entregables aprobados (si_no -> seleccion_unica)
UPDATE proyecta_db.project_closure_question
   SET tipo_respuesta = 'seleccion_unica',
       opciones = '["Sí","No"]'::jsonb,
       orden = 7,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿Todos los entregables fueron aprobados?';

-- orden 8 -> Impactos esperados (si_no_detallado -> seleccion_unica)
UPDATE proyecta_db.project_closure_question
   SET tipo_respuesta = 'seleccion_unica',
       opciones = '["Sí","No","Parcialmente"]'::jsonb,
       orden = 8,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿El proyecto generó los impactos esperados?';

-- orden 9 -> Desviaciones presupuestales (si_no_detallado -> seleccion_unica)
UPDATE proyecta_db.project_closure_question
   SET tipo_respuesta = 'seleccion_unica',
       opciones = '["Sí","No"]'::jsonb,
       orden = 9,
       activo = TRUE,
       updated_by = 'migration-v4',
       actualizado_en = CURRENT_TIMESTAMP
 WHERE texto = '¿Existieron desviaciones significativas en el presupuesto?';

-- -----------------------------------------------------------------------------
-- 2) Inserta las preguntas del formato A-GT-FR-004 que falten
--     (orden 1-5 alimentan el acta; 6-15 son el checklist complementario)
-- -----------------------------------------------------------------------------
INSERT INTO proyecta_db.project_closure_question
    (texto, tipo_respuesta, opciones, activo, orden, created_by, updated_by)
SELECT v.texto,
       v.tipo_respuesta,
       v.opciones::jsonb,
       TRUE,
       v.orden,
       'migration-v4',
       'migration-v4'
FROM (VALUES
    -- === Secciones que alimentan el Acta de Cierre (orden 1-5) ===
    ('Describa el resumen ejecutivo del proyecto al momento del cierre.',
        'texto_libre', NULL::text, 1),
    ('Describa los aspectos positivos del proyecto (qué funcionó bien).',
        'texto_libre', NULL::text, 2),
    ('Describa los aspectos a mejorar del proyecto (qué no funcionó).',
        'texto_libre', NULL::text, 3),
    ('¿Qué recomendaciones haría para futuros proyectos similares?',
        'texto_libre', NULL::text, 4),
    ('Describa las actividades de transferencia de conocimiento realizadas al cierre.',
        'texto_libre', NULL::text, 5),
    -- === Resultados ===
    ('¿Se cumplieron todos los objetivos del proyecto?',
        'seleccion_unica', '["Sí","No"]', 6),
    ('¿Todos los entregables fueron aprobados?',
        'seleccion_unica', '["Sí","No"]', 7),
    ('¿El proyecto generó los impactos esperados?',
        'seleccion_unica', '["Sí","No","Parcialmente"]', 8),
    ('¿Existieron desviaciones significativas en el presupuesto?',
        'seleccion_unica', '["Sí","No"]', 9),
    ('Describa el alcance final ejecutado del proyecto.',
        'texto_libre', NULL::text, 10),
    -- === Cierre administrativo ===
    ('Detalle los documentos y entregables finales entregados al cierre.',
        'texto_libre', NULL::text, 11),
    ('Detalle los pendientes o compromisos abiertos al momento del cierre.',
        'texto_libre', NULL::text, 12),
    ('¿La documentación inicial del proyecto se encuentra completa y verificada (viabilidad, cronograma, acta de constitución y plan de comunicaciones)?',
        'seleccion_unica', '["Sí","No"]', 13),
    -- === Riesgos y observaciones ===
    ('¿Los riesgos abiertos al cierre fueron mitigados o asumidos por un responsable?',
        'seleccion_unica', '["Sí","No","Parcialmente"]', 14),
    ('Detalle otras observaciones relevantes para el cierre del proyecto.',
        'texto_libre', NULL::text, 15)
) AS v(texto, tipo_respuesta, opciones, orden)
WHERE NOT EXISTS (
    SELECT 1 FROM proyecta_db.project_closure_question q
     WHERE q.texto = v.texto
);
