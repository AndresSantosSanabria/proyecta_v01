INSERT INTO proyecta_db.lista_parametrica_config (lista_clave, item_codigo, item_nombre, orden, activo, lista_nombre_campo, lista_tipo)
VALUES
  ('ROL_EQUIPO', 'ANALISTA_SISTEMAS', 'Analista de Sistemas', 1, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'DESARROLLADOR_SENIOR', 'Desarrollador Senior', 2, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'DESARROLLADOR_JUNIOR', 'Desarrollador Junior', 3, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'LIDER_TECNICO', 'Lider Tecnico', 4, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'ARQUITECTO_SOFTWARE', 'Arquitecto de Software', 5, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'ADMIN_BD', 'Administrador de Base de Datos', 6, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'ING_INFRAESTRUCTURA', 'Ingeniero de Infraestructura', 7, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'TESTER_QA', 'Tester / QA', 8, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'SCRUM_MASTER', 'Scrum Master', 9, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'PRODUCT_OWNER', 'Product Owner', 10, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'ANALISTA_SEGURIDAD', 'Analista de Seguridad', 11, true, 'Rol en el Proyecto', 'Lista'),
  ('ROL_EQUIPO', 'CONSULTOR_FUNCIONAL', 'Consultor Funcional', 12, true, 'Rol en el Proyecto', 'Lista')
ON CONFLICT (lista_clave, item_codigo) DO NOTHING;
