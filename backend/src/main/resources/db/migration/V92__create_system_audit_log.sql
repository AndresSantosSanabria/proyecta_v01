-- ========================================================
-- Modulo de Auditoria de Logs de Sistema
-- Crea la tabla system_audit_log para registrar todas las
-- peticiones HTTP interceptadas por el AuditingAspect (AOP),
-- e incorpora el permiso AUDITORIA:VER (asignado a admin).
-- ========================================================

CREATE TABLE IF NOT EXISTS proyecta_db.system_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id VARCHAR(100),
    usuario_nombre VARCHAR(255),
    usuario_rol VARCHAR(100),
    accion VARCHAR(50) NOT NULL,
    modulo VARCHAR(150) NOT NULL,
    metodo_http VARCHAR(10),
    recurso VARCHAR(255),
    codigo_estado INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    detalle TEXT,
    traza_error TEXT,
    ip_origen VARCHAR(45),
    user_agent TEXT,
    duracion_ms BIGINT,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMP,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_fecha ON proyecta_db.system_audit_log (fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_usuario ON proyecta_db.system_audit_log (usuario_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_accion ON proyecta_db.system_audit_log (accion);
CREATE INDEX IF NOT EXISTS idx_audit_log_estado ON proyecta_db.system_audit_log (estado);
CREATE INDEX IF NOT EXISTS idx_audit_log_modulo ON proyecta_db.system_audit_log (modulo);
CREATE INDEX IF NOT EXISTS idx_audit_log_eliminado ON proyecta_db.system_audit_log (eliminado);

-- Incorporar el permiso AUDITORIA:VER y asignarlo al rol admin.
INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion, activo)
VALUES (
    'AUDITORIA:VER',
    'Ver auditoria',
    'Permite consultar el registro de auditoria de acciones y logs del sistema',
    TRUE
)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo = 'AUDITORIA:VER'
WHERE r.codigo = 'admin'
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;