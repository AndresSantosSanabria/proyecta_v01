-- Flyway reboot pack for security-related schema.
-- Consolidates the current security migrations into a single idempotent script.

CREATE SCHEMA IF NOT EXISTS proyecta_db;

-- Local role catalog used by the application authorization layer.
CREATE TABLE IF NOT EXISTS proyecta_db.rol_config (
    rol_id           SERIAL PRIMARY KEY,
    codigo           VARCHAR(30)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    descripcion      VARCHAR(300),
    nivel_acceso     INTEGER      NOT NULL DEFAULT 0,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.rol_config (codigo, nombre, descripcion, nivel_acceso, activo)
VALUES (
    'ADMINISTRADOR',
    'Administrador',
    'Acceso total al sistema',
    100,
    TRUE
)
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    nivel_acceso = EXCLUDED.nivel_acceso,
    activo = TRUE;

ALTER TABLE IF EXISTS proyecta_db.usuario
    ADD COLUMN IF NOT EXISTS rol_config_id INTEGER REFERENCES proyecta_db.rol_config(rol_id);

DO $$
BEGIN
    IF to_regclass('proyecta_db.usuario') IS NOT NULL THEN
        WITH admin_role AS (
            SELECT rol_id
            FROM proyecta_db.rol_config
            WHERE codigo = 'ADMINISTRADOR'
            LIMIT 1
        )
        INSERT INTO proyecta_db.usuario (
            keycloak_sub,
            nombre,
            correo,
            contrasena_hash,
            rol,
            rol_config_id,
            activo
        )
        SELECT
            'fabio.santos@cundinamarca.gov.co',
            'Fabio Santos',
            'fabio.santos@cundinamarca.gov.co',
            'hash_simulado',
            'ADMINISTRADOR',
            admin_role.rol_id,
            TRUE
        FROM admin_role
        ON CONFLICT (correo) DO UPDATE
        SET
            keycloak_sub = EXCLUDED.keycloak_sub,
            nombre = EXCLUDED.nombre,
            contrasena_hash = EXCLUDED.contrasena_hash,
            rol = EXCLUDED.rol,
            rol_config_id = EXCLUDED.rol_config_id,
            activo = TRUE;
    END IF;
END $$;

COMMENT ON COLUMN proyecta_db.usuario.rol_config_id IS
    'Rol local asociado. El usuario fabio.santos@cundinamarca.gov.co debe quedar como ADMINISTRADOR.';

-- Security catalog used by the new administration module.
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

INSERT INTO proyecta_db.roles (codigo, nombre, descripcion, transversal, activo)
VALUES
    ('admin', 'Administrador', 'Control total de la plataforma', TRUE, TRUE),
    ('gestor_tic', 'Gestor TIC', 'Administracion tecnica y transversal', TRUE, TRUE),
    ('director_proyecto', 'Director de Proyecto', 'Operacion sobre sus proyectos asignados', FALSE, TRUE),
    ('auditor', 'Auditor', 'Consulta y revision sin edicion', FALSE, TRUE),
    ('consulta', 'Consulta', 'Solo lectura', FALSE, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    transversal = EXCLUDED.transversal,
    activo = TRUE;

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
VALUES
    ('PROYECTO:VER', 'Ver proyecto', 'Permite consultar el detalle de proyectos', TRUE),
    ('PROYECTO:CREAR', 'Crear proyecto', 'Permite crear proyectos nuevos', TRUE),
    ('PROYECTO:EDITAR', 'Editar proyecto', 'Permite editar proyectos existentes', TRUE),
    ('PROYECTO:CERRAR', 'Cerrar proyecto', 'Permite cerrar proyectos', TRUE),
    ('ENTREGABLE:APROBAR', 'Aprobar entregable', 'Permite marcar entregables como conformes', TRUE),
    ('EVIDENCIA:CARGAR', 'Cargar evidencia', 'Permite subir evidencias PDF', TRUE),
    ('DOCUMENTO:CARGAR', 'Cargar documento', 'Permite subir documentos de soporte', TRUE),
    ('CRONOGRAMA:CARGAR', 'Cargar cronograma', 'Permite subir el PDF del cronograma', TRUE),
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
  AND p.codigo IN ('PROYECTO:VER', 'PROYECTO:CREAR', 'PROYECTO:EDITAR', 'PROYECTO:CERRAR',
                   'ENTREGABLE:APROBAR', 'EVIDENCIA:CARGAR', 'DOCUMENTO:CARGAR',
                   'CRONOGRAMA:CARGAR', 'SISTEMA:CONFIGURAR')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('PROYECTO:VER', 'ENTREGABLE:APROBAR', 'EVIDENCIA:CARGAR', 'DOCUMENTO:CARGAR', 'CRONOGRAMA:CARGAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo = 'PROYECTO:VER'
WHERE r.codigo IN ('auditor', 'consulta')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;

COMMENT ON TABLE proyecta_db.rol_permiso IS 'Matriz dinamica de asignacion de permisos a roles en proyecta_db.';

-- Local security users table used by the admin UI.
CREATE TABLE IF NOT EXISTS proyecta_db.usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(120) NOT NULL UNIQUE,
    username VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(180) NOT NULL,
    correo VARCHAR(200) NOT NULL UNIQUE,
    dependencia VARCHAR(180),
    rol_codigo VARCHAR(120),
    rol_nombre VARCHAR(180),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

ALTER TABLE proyecta_db.usuarios
    ADD COLUMN IF NOT EXISTS rol_codigo VARCHAR(120);

ALTER TABLE proyecta_db.usuarios
    ADD COLUMN IF NOT EXISTS rol_nombre VARCHAR(180);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON proyecta_db.usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON proyecta_db.usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol_codigo ON proyecta_db.usuarios(rol_codigo);

COMMENT ON TABLE proyecta_db.usuarios IS 'Usuarios sincronizados desde Keycloak para control de acceso y asignaciones por proyecto';
COMMENT ON COLUMN proyecta_db.usuarios.rol_codigo IS 'Codigo del rol asignado al usuario de seguridad';
COMMENT ON COLUMN proyecta_db.usuarios.rol_nombre IS 'Nombre legible del rol asignado al usuario de seguridad';

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

CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_usuario ON proyecta_db.usuario_proyecto(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_proyecto ON proyecta_db.usuario_proyecto(proyecto_id);

COMMENT ON TABLE proyecta_db.usuario_proyecto IS 'Asignacion explicita de usuarios a proyectos y cargos';

-- Align current project schema with the active Entregable entity.
ALTER TABLE IF EXISTS proyecta_db.entregable
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(300);

COMMENT ON COLUMN proyecta_db.entregable.descripcion IS
    'Descripcion opcional del entregable usada por el modelo de dominio actual.';

-- Align project team schema with the active Proyecto entity collection mapping.
ALTER TABLE IF EXISTS proyecta_db.proyecto_equipo
    ADD COLUMN IF NOT EXISTS miembro_cargo VARCHAR(100);

COMMENT ON COLUMN proyecta_db.proyecto_equipo.miembro_cargo IS
    'Cargo del miembro del equipo asociado al proyecto.';
