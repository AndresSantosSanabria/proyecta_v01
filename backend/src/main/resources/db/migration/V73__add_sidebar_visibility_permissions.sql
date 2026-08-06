-- Permisos de visibilidad del Sidebar
-- Estos permisos controlan que elementos del menu lateral se muestran a cada usuario

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo, fecha_creacion) VALUES
('SIDEBAR:DASHBOARD',   'Mostrar Dashboard en menu',   'Controla la visibilidad del modulo Dashboard en el sidebar', true, NOW()),
('SIDEBAR:PROYECTOS',   'Mostrar Proyectos en menu',   'Controla la visibilidad del modulo Proyectos en el sidebar', true, NOW()),
('SIDEBAR:REPORTES',    'Mostrar Reportes en menu',    'Controla la visibilidad del modulo Reportes en el sidebar', true, NOW()),
('SIDEBAR:ANALITICAS',  'Mostrar Analiticas en menu',  'Controla la visibilidad del modulo Analiticas en el sidebar', true, NOW()),
('SIDEBAR:SEGURIDAD',   'Mostrar Configuracion Seguridad en menu', 'Controla la visibilidad del modulo Configuracion y Seguridad en el sidebar', true, NOW())
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

-- Asignar permisos SIDEBAR a todos los roles que ya tenian el permiso funcional equivalente
-- Admin: todos los SIDEBAR
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'admin' AND p.codigo LIKE 'SIDEBAR:%'
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- gestor_tic: todos los SIDEBAR
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'gestor_tic' AND p.codigo LIKE 'SIDEBAR:%'
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- gestor_proyectos: todos los SIDEBAR
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'gestor_proyectos' AND p.codigo LIKE 'SIDEBAR:%'
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- director_proyecto: DASHBOARD, PROYECTOS (no tiene REPORTES ni ANALITICAS ni SEGURIDAD)
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'director_proyecto'
  AND p.codigo IN ('SIDEBAR:DASHBOARD', 'SIDEBAR:PROYECTOS')
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- auditor: DASHBOARD, PROYECTOS, REPORTES, ANALITICAS
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'auditor'
  AND p.codigo IN ('SIDEBAR:DASHBOARD', 'SIDEBAR:PROYECTOS', 'SIDEBAR:REPORTES', 'SIDEBAR:ANALITICAS')
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- consulta: DASHBOARD, PROYECTOS, REPORTES, ANALITICAS
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'consulta'
  AND p.codigo IN ('SIDEBAR:DASHBOARD', 'SIDEBAR:PROYECTOS', 'SIDEBAR:REPORTES', 'SIDEBAR:ANALITICAS')
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;

-- visualizador: DASHBOARD, PROYECTOS
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo, fecha_creacion)
SELECT r.id, p.id, TRUE, NOW()
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'visualizador'
  AND p.codigo IN ('SIDEBAR:DASHBOARD', 'SIDEBAR:PROYECTOS')
ON CONFLICT (rol_id, permiso_id) DO UPDATE SET activo = TRUE;
