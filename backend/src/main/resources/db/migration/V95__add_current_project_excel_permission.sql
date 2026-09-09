INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
VALUES (
    'REPORTE:DESCARGAR_ACTUAL',
    'Descargar reporte actual del proyecto',
    'Permite descargar el reporte Excel actualizado de cada proyecto',
    TRUE
)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT rol.id, permiso.id, TRUE
FROM proyecta_db.roles rol
JOIN proyecta_db.permisos permiso ON permiso.codigo = 'REPORTE:DESCARGAR_ACTUAL'
WHERE rol.codigo IN ('admin', 'gestor_tic', 'gestor_proyectos')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
