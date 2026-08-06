-- V72: Crear rol VISUALIZADOR en ambos sistemas de seguridad
-- Rol por defecto para usuarios nuevos autenticados via Keycloak
-- NO tiene permisos asignados (principio de menor privilegio)

-- 1. Insertar en el catalogo de seguridad (tabla roles)
INSERT INTO proyecta_db.roles (codigo, nombre, descripcion, transversal, activo)
VALUES ('visualizador', 'Visualizador', 'Acceso de solo lectura por defecto', false, true)
ON CONFLICT (codigo) DO NOTHING;

-- 2. Insertar en el sistema legacy (tabla rol_config) para mantener consistencia
INSERT INTO proyecta_db.rol_config (codigo, nombre, descripcion, nivel_acceso, activo)
VALUES ('VISUALIZADOR', 'Visualizador', 'Acceso de solo lectura por defecto', 10, true)
ON CONFLICT (codigo) DO NOTHING;

-- NOTA: No se insertan registros en rol_permiso para este rol.
-- Esto garantiza 0 permisos habilitados por defecto.
