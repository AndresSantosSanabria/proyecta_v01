INSERT INTO proyecta_db.lista_parametrica_config (lista_clave, item_codigo, item_nombre, orden, activo, lista_nombre_campo, lista_tipo)
VALUES
  ('ROL_USUARIO', 'admin', 'Administrador', 1, true, 'Rol de Usuario', 'Lista'),
  ('ROL_USUARIO', 'gestor_tic', 'Gestor TIC', 2, true, 'Rol de Usuario', 'Lista'),
  ('ROL_USUARIO', 'gestor_proyectos', 'Gestor de Proyectos', 3, true, 'Rol de Usuario', 'Lista'),
  ('ROL_USUARIO', 'director_proyecto', 'Director de Proyecto', 4, true, 'Rol de Usuario', 'Lista'),
  ('ROL_USUARIO', 'auditor', 'Auditor', 5, true, 'Rol de Usuario', 'Lista'),
  ('ROL_USUARIO', 'consulta', 'Consulta', 6, true, 'Rol de Usuario', 'Lista')
ON CONFLICT (lista_clave, item_codigo) DO NOTHING;
