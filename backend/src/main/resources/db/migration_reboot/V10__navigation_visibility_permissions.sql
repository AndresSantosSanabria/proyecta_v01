-- Agrega permisos de visibilidad para navegación y accesos principales de la UI.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
VALUES
    ('DASHBOARD:VER', 'Ver dashboard', 'Permite consultar la portada y resumen principal del sistema', TRUE),
    ('REPORTE:VER', 'Ver reportes', 'Permite acceder al modulo de reportes', TRUE),
    ('ANALITICA:VER', 'Ver analiticas', 'Permite acceder al modulo de analiticas', TRUE),
    ('CONFIGURACION:VER', 'Ver configuracion', 'Permite mostrar la pantalla de administracion y seguridad', TRUE)
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN (
    'DASHBOARD:VER', 'REPORTE:VER', 'ANALITICA:VER', 'CONFIGURACION:VER'
)
WHERE r.codigo IN ('admin', 'gestor_tic')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('DASHBOARD:VER', 'REPORTE:VER', 'ANALITICA:VER')
WHERE r.codigo IN ('director_proyecto', 'auditor', 'consulta')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
