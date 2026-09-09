-- V98: Agrega permiso funcional para cierre extraordinario de proyectos.
-- Permite a Administradores y Gestores de Proyectos cerrar proyectos
-- omitiendo las validaciones estandar del flujo de cierre.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES (
    'CIERRE:EXTRAORDINARIO',
    'Cierre extraordinario',
    'Permite cerrar un proyecto de forma extraordinaria, omitiendo las validaciones estandar del flujo de cierre. Esta accion es irreversible.'
)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

-- Asignar el permiso a Administradores y Gestores de Proyectos
INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo = 'CIERRE:EXTRAORDINARIO'
WHERE r.codigo IN ('admin', 'gestor_proyectos')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
