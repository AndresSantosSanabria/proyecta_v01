-- Ensure the security users table exposes the role columns expected by the admin UI.

ALTER TABLE IF EXISTS proyecta_db.usuarios
    ADD COLUMN IF NOT EXISTS rol_codigo VARCHAR(120);

ALTER TABLE IF EXISTS proyecta_db.usuarios
    ADD COLUMN IF NOT EXISTS rol_nombre VARCHAR(180);

CREATE INDEX IF NOT EXISTS idx_usuarios_rol_codigo ON proyecta_db.usuarios(rol_codigo);

COMMENT ON COLUMN proyecta_db.usuarios.rol_codigo IS
    'Codigo del rol asignado al usuario de seguridad';

COMMENT ON COLUMN proyecta_db.usuarios.rol_nombre IS
    'Nombre legible del rol asignado al usuario de seguridad';
