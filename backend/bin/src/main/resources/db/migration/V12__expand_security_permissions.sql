-- V12: Expande el catalogo de seguridad para cubrir la matriz completa de privilegios.
-- Esta migracion actualiza instalaciones existentes sin requerir reinstalacion.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('DASHBOARD:VER', 'Ver dashboard', 'Permite consultar la portada y resumen principal del sistema'),
    ('PROYECTO:VER', 'Ver proyecto', 'Permite consultar el detalle de proyectos'),
    ('PROYECTO:CREAR', 'Crear proyecto', 'Permite crear proyectos nuevos'),
    ('PROYECTO:EDITAR', 'Editar proyecto', 'Permite editar proyectos existentes'),
    ('PROYECTO:CERRAR', 'Cerrar proyecto', 'Permite cerrar proyectos'),
    ('REPORTE:VER', 'Ver reportes', 'Permite acceder al modulo de reportes'),
    ('ANALITICA:VER', 'Ver analiticas', 'Permite acceder al modulo de analiticas'),
    ('CONFIGURACION:VER', 'Ver configuracion', 'Permite mostrar la pantalla de administracion y seguridad'),
    ('ENTREGABLE:VER', 'Ver entregable', 'Permite consultar el detalle de entregables'),
    ('ENTREGABLE:CREAR', 'Crear entregable', 'Permite registrar entregables nuevos'),
    ('ENTREGABLE:EDITAR', 'Editar entregable', 'Permite modificar entregables existentes'),
    ('ENTREGABLE:APROBAR', 'Aprobar entregable', 'Permite marcar entregables como conformes'),
    ('EVIDENCIA:VER', 'Ver evidencia', 'Permite consultar evidencias registradas'),
    ('EVIDENCIA:CARGAR', 'Cargar evidencia', 'Permite subir evidencias PDF'),
    ('EVIDENCIA:EDITAR', 'Editar evidencia', 'Permite actualizar evidencias existentes'),
    ('EVIDENCIA:ELIMINAR', 'Eliminar evidencia', 'Permite eliminar evidencias registradas'),
    ('DOCUMENTO:VER', 'Ver documento', 'Permite consultar documentos registrados'),
    ('DOCUMENTO:CARGAR', 'Cargar documento', 'Permite subir documentos de soporte'),
    ('DOCUMENTO:EDITAR', 'Editar documento', 'Permite actualizar documentos existentes'),
    ('DOCUMENTO:ELIMINAR', 'Eliminar documento', 'Permite eliminar documentos registrados'),
    ('CRONOGRAMA:VER', 'Ver cronograma', 'Permite consultar cronogramas registrados'),
    ('CRONOGRAMA:CARGAR', 'Cargar cronograma', 'Permite subir el PDF del cronograma'),
    ('CRONOGRAMA:EDITAR', 'Editar cronograma', 'Permite actualizar cronogramas existentes'),
    ('CRONOGRAMA:ELIMINAR', 'Eliminar cronograma', 'Permite eliminar cronogramas registrados'),
    ('SISTEMA:VER', 'Ver sistema', 'Permite consultar la configuracion general del sistema'),
    ('SISTEMA:CREAR', 'Crear configuracion del sistema', 'Permite registrar configuraciones de sistema'),
    ('SISTEMA:EDITAR', 'Editar sistema', 'Permite actualizar la configuracion general del sistema'),
    ('SISTEMA:CONFIGURAR', 'Configurar sistema', 'Permite administrar usuarios, roles y permisos')
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
CROSS JOIN proyecta_db.permisos p
WHERE r.codigo IN ('admin', 'gestor_tic')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN (
    'DASHBOARD:VER', 'PROYECTO:VER', 'PROYECTO:EDITAR', 'REPORTE:VER', 'ANALITICA:VER',
    'ENTREGABLE:VER', 'ENTREGABLE:CREAR', 'ENTREGABLE:EDITAR', 'ENTREGABLE:APROBAR',
    'EVIDENCIA:VER', 'EVIDENCIA:CARGAR',
    'DOCUMENTO:VER', 'DOCUMENTO:CARGAR',
    'CRONOGRAMA:VER', 'CRONOGRAMA:CARGAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'REPORTE:VER', 'ANALITICA:VER', 'ENTREGABLE:VER', 'EVIDENCIA:VER', 'DOCUMENTO:VER', 'CRONOGRAMA:VER', 'SISTEMA:VER')
WHERE r.codigo IN ('auditor', 'consulta')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
