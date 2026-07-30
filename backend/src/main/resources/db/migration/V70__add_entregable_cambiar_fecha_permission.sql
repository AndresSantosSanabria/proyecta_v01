-- V68: Agrega permiso ENTREGABLE:CAMBIAR_FECHA para modificar la fecha limite de entregables.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('ENTREGABLE:CAMBIAR_FECHA', 'Cambiar fecha limite', 'Permite modificar la fecha limite de un entregable existente con justificacion y soporte')
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo = 'ENTREGABLE:CAMBIAR_FECHA'
WHERE r.codigo IN ('admin', 'gestor_tic', 'gestor_proyectos')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
