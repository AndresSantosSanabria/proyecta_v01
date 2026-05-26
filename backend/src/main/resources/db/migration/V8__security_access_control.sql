CREATE SCHEMA IF NOT EXISTS proyecta_db;

CREATE TABLE IF NOT EXISTS proyecta_db.usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(120) NOT NULL UNIQUE,
    username VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(180) NOT NULL,
    correo VARCHAR(200) NOT NULL UNIQUE,
    dependencia VARCHAR(180),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.roles (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300),
    transversal BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.permisos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(400),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.rol_permiso (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES proyecta_db.roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES proyecta_db.permisos(id) ON DELETE CASCADE,
    CONSTRAINT uk_rol_permiso UNIQUE (rol_id, permiso_id)
);

CREATE TABLE IF NOT EXISTS proyecta_db.usuario_proyecto (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    proyecto_id VARCHAR(30) NOT NULL,
    cargo VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_proyecto_usuario FOREIGN KEY (usuario_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    CONSTRAINT uk_usuario_proyecto UNIQUE (usuario_id, proyecto_id, cargo)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON proyecta_db.usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON proyecta_db.usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_roles_codigo ON proyecta_db.roles(codigo);
CREATE INDEX IF NOT EXISTS idx_permisos_codigo ON proyecta_db.permisos(codigo);
CREATE INDEX IF NOT EXISTS idx_rol_permiso_rol ON proyecta_db.rol_permiso(rol_id);
CREATE INDEX IF NOT EXISTS idx_rol_permiso_permiso ON proyecta_db.rol_permiso(permiso_id);
CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_usuario ON proyecta_db.usuario_proyecto(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_proyecto ON proyecta_db.usuario_proyecto(proyecto_id);

INSERT INTO proyecta_db.roles (codigo, nombre, descripcion, transversal)
VALUES
    ('admin', 'Administrador', 'Control total de la plataforma', TRUE),
    ('gestor_tic', 'Gestor TIC', 'Administracion tecnica y transversal', TRUE),
    ('director_proyecto', 'Director de Proyecto', 'Operacion sobre sus proyectos asignados', FALSE),
    ('auditor', 'Auditor', 'Consulta y revision sin edicion', FALSE),
    ('consulta', 'Consulta', 'Solo lectura', FALSE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('PROYECTO:VER', 'Ver proyecto', 'Permite consultar el detalle de proyectos'),
    ('PROYECTO:CREAR', 'Crear proyecto', 'Permite crear proyectos nuevos'),
    ('PROYECTO:EDITAR', 'Editar proyecto', 'Permite editar proyectos existentes'),
    ('PROYECTO:CERRAR', 'Cerrar proyecto', 'Permite cerrar proyectos'),
    ('ENTREGABLE:APROBAR', 'Aprobar entregable', 'Permite marcar entregables como conformes'),
    ('EVIDENCIA:CARGAR', 'Cargar evidencia', 'Permite subir evidencias PDF'),
    ('DOCUMENTO:CARGAR', 'Cargar documento', 'Permite subir documentos de soporte'),
    ('CRONOGRAMA:CARGAR', 'Cargar cronograma', 'Permite subir el PDF del cronograma'),
    ('SISTEMA:CONFIGURAR', 'Configurar sistema', 'Permite administrar usuarios, roles y permisos')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
CROSS JOIN proyecta_db.permisos p
WHERE r.codigo IN ('admin', 'gestor_tic')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('PROYECTO:VER', 'ENTREGABLE:APROBAR', 'EVIDENCIA:CARGAR', 'DOCUMENTO:CARGAR', 'CRONOGRAMA:CARGAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('PROYECTO:VER')
WHERE r.codigo = 'auditor'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('PROYECTO:VER')
WHERE r.codigo = 'consulta'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

COMMENT ON TABLE proyecta_db.usuarios IS 'Usuarios sincronizados desde Keycloak para control de acceso y asignaciones por proyecto';
COMMENT ON TABLE proyecta_db.roles IS 'Catalogo dinamico de roles de negocio';
COMMENT ON TABLE proyecta_db.permisos IS 'Catalogo dinamico de permisos atomicos';
COMMENT ON TABLE proyecta_db.rol_permiso IS 'Matriz dinamica de asignacion de permisos a roles';
COMMENT ON TABLE proyecta_db.usuario_proyecto IS 'Asignacion explicita de usuarios a proyectos y cargos';
