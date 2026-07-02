-- Expande el catalogo de seguridad para cubrir la matriz completa de privilegios.

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
VALUES
    ('DASHBOARD:VER', 'Ver dashboard', 'Permite consultar la portada y resumen principal del sistema', TRUE),
    ('PROYECTO:VER', 'Ver proyecto', 'Permite consultar el detalle de proyectos', TRUE),
    ('PROYECTO:CREAR', 'Crear proyecto', 'Permite crear proyectos nuevos', TRUE),
    ('PROYECTO:EDITAR', 'Editar proyecto', 'Permite editar proyectos existentes', TRUE),
    ('PROYECTO:CERRAR', 'Cerrar proyecto', 'Permite cerrar proyectos', TRUE),
    ('REPORTE:VER', 'Ver reportes', 'Permite acceder al modulo de reportes', TRUE),
    ('ANALITICA:VER', 'Ver analiticas', 'Permite acceder al modulo de analiticas', TRUE),
    ('CONFIGURACION:VER', 'Ver configuracion', 'Permite mostrar la pantalla de administracion y seguridad', TRUE),
    ('ENTREGABLE:VER', 'Ver entregable', 'Permite consultar el detalle de entregables', TRUE),
    ('ENTREGABLE:CREAR', 'Crear entregable', 'Permite registrar entregables nuevos', TRUE),
    ('ENTREGABLE:EDITAR', 'Editar entregable', 'Permite modificar entregables existentes', TRUE),
    ('ENTREGABLE:APROBAR', 'Aprobar entregable', 'Permite marcar entregables como conformes', TRUE),
    ('AVANCE:VER', 'Ver avance', 'Permite consultar el avance consolidado del proyecto', TRUE),
    ('AVANCE:EDITAR', 'Editar avance', 'Permite registrar y actualizar avances del proyecto', TRUE),
    ('AVANCE:APROBAR', 'Aprobar avance', 'Permite validar avances enviados por el Director de Proyecto', TRUE),
    ('EVIDENCIA:VER', 'Ver evidencia', 'Permite consultar evidencias registradas', TRUE),
    ('EVIDENCIA:CARGAR', 'Cargar evidencia', 'Permite subir evidencias PDF', TRUE),
    ('EVIDENCIA:EDITAR', 'Editar evidencia', 'Permite actualizar evidencias existentes', TRUE),
    ('EVIDENCIA:ELIMINAR', 'Eliminar evidencia', 'Permite eliminar evidencias registradas', TRUE),
    ('DOCUMENTO:VER', 'Ver documento', 'Permite consultar documentos registrados', TRUE),
    ('DOCUMENTO:CARGAR', 'Cargar documento', 'Permite subir documentos de soporte', TRUE),
    ('DOCUMENTO:EDITAR', 'Editar documento', 'Permite actualizar documentos existentes', TRUE),
    ('DOCUMENTO:ELIMINAR', 'Eliminar documento', 'Permite eliminar documentos registrados', TRUE),
    ('CRONOGRAMA:VER', 'Ver cronograma', 'Permite consultar cronogramas registrados', TRUE),
    ('CRONOGRAMA:CARGAR', 'Cargar cronograma', 'Permite subir el PDF del cronograma', TRUE),
    ('CRONOGRAMA:EDITAR', 'Editar cronograma', 'Permite actualizar cronogramas existentes', TRUE),
    ('CRONOGRAMA:ELIMINAR', 'Eliminar cronograma', 'Permite eliminar cronogramas registrados', TRUE),
    ('CIERRE:SOLICITAR', 'Solicitar cierre', 'Permite enviar la solicitud de cierre del proyecto', TRUE),
    ('CIERRE:APROBAR', 'Aprobar cierre', 'Permite validar y aprobar el cierre del proyecto', TRUE),
    ('SISTEMA:VER', 'Ver sistema', 'Permite consultar la configuracion general del sistema', TRUE),
    ('SISTEMA:CREAR', 'Crear configuracion del sistema', 'Permite registrar configuraciones de sistema', TRUE),
    ('SISTEMA:EDITAR', 'Editar sistema', 'Permite actualizar la configuracion general del sistema', TRUE),
    ('SISTEMA:CONFIGURAR', 'Configurar sistema', 'Permite administrar usuarios, roles y permisos', TRUE)
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
    'AVANCE:VER', 'AVANCE:EDITAR',
    'EVIDENCIA:VER', 'EVIDENCIA:CARGAR',
    'DOCUMENTO:VER', 'DOCUMENTO:CARGAR',
    'CRONOGRAMA:VER', 'CRONOGRAMA:CARGAR',
    'CIERRE:SOLICITAR')
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_proyecto_director_activo
    ON proyecta_db.usuario_proyecto (proyecto_id)
    WHERE UPPER(cargo) = 'DIRECTOR_PROYECTO' AND activo = TRUE;
