INSERT INTO proyecta_db.tipo_documento_config (codigo, nombre, descripcion, require_pdf, orden, activo, fecha_creacion)
VALUES
  ('VIABILIZACION', 'Documento de Viabilidad', 'Documento que respalda la viabilidad del proyecto', true, 1, true, NOW()),
  ('ACTA_CONSTITUCION', 'Acta de Constitucion', 'Acta de constitucion del proyecto', true, 2, true, NOW()),
  ('CRONOGRAMA', 'Cronograma del Proyecto', 'Cronograma general del proyecto', true, 3, true, NOW()),
  ('PLAN_COMUNICACIONES', 'Plan de Comunicaciones', 'Plan de comunicaciones del proyecto', true, 4, true, NOW())
ON CONFLICT (codigo) DO NOTHING;
