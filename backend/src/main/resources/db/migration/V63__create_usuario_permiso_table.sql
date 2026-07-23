-- V63: Tabla de permisos individuales por usuario (override de matriz de rol)
-- Permite personalizar permisos por usuario, similar a Redmine workflow permissions

CREATE TABLE IF NOT EXISTS proyecta_db.usuario_permiso (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE,
    permiso_id      BIGINT NOT NULL REFERENCES proyecta_db.permisos(id) ON DELETE CASCADE,
    concedido       BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_modificacion TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, permiso_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_permiso_usuario ON proyecta_db.usuario_permiso(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_permiso_permiso ON proyecta_db.usuario_permiso(permiso_id);

COMMENT ON TABLE proyecta_db.usuario_permiso IS 'Permisos individuales por usuario. Sobreescriben los permisos del rol cuando estan presentes.';
COMMENT ON COLUMN proyecta_db.usuario_permiso.concedido IS 'TRUE = permiso concedido, FALSE = permiso denegado explícitamente';
