-- V97: Eliminar tablas legacy usuario y rol_config
-- Todas las referencias ahora apuntan a proyecta_db.usuarios (SeguridadUsuario)

-- Eliminar trigger y funciones dependientes de usuario (si existieran)
DROP TRIGGER IF EXISTS trg_usuario_updated ON proyecta_db.usuario;
DROP FUNCTION IF EXISTS proyecta_db.update_usuario_updated_at();

-- Eliminar tabla usuario legacy (ya no se usa en el codigo)
DROP TABLE IF EXISTS proyecta_db.usuario CASCADE;

-- Eliminar tabla rol_config legacy (ya no se usa en el codigo)
DROP TABLE IF EXISTS proyecta_db.rol_config CASCADE;

-- Eliminar tipo enum rol si existe
DROP TYPE IF EXISTS proyecta_db.rol_enum CASCADE;
