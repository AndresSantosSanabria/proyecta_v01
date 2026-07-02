-- V13: Agrega permisos de avance y cierre sin alterar migraciones ya aplicadas.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('AVANCE:VER', 'Ver avance', 'Permite consultar el avance consolidado del proyecto'),
    ('AVANCE:EDITAR', 'Editar avance', 'Permite registrar y actualizar avances del proyecto'),
    ('AVANCE:APROBAR', 'Aprobar avance', 'Permite validar avances enviados por el Director de Proyecto'),
    ('CIERRE:SOLICITAR', 'Solicitar cierre', 'Permite enviar la solicitud de cierre del proyecto'),
    ('CIERRE:APROBAR', 'Aprobar cierre', 'Permite validar y aprobar el cierre del proyecto')
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN (
    'AVANCE:VER', 'AVANCE:EDITAR', 'AVANCE:APROBAR',
    'CIERRE:SOLICITAR', 'CIERRE:APROBAR'
)
WHERE r.codigo IN ('admin', 'gestor_tic')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('AVANCE:VER', 'AVANCE:EDITAR', 'CIERRE:SOLICITAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('CRONOGRAMA:CARGAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
