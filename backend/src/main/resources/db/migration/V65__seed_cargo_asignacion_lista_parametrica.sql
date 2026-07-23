INSERT INTO proyecta_db.lista_parametrica_config (lista_clave, item_codigo, item_nombre, orden, activo)
VALUES
  ('CARGO_ASIGNACION', 'DIRECTOR_PROYECTO', 'Director de Proyecto', 1, true),
  ('CARGO_ASIGNACION', 'LIDER_TECNICO', 'Lider Tecnico', 2, true),
  ('CARGO_ASIGNACION', 'ANALISTA', 'Analista', 3, true),
  ('CARGO_ASIGNACION', 'APOYO', 'Apoyo', 4, true)
ON CONFLICT (lista_clave, item_codigo) DO NOTHING;
