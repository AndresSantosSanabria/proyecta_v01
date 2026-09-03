CREATE TABLE IF NOT EXISTS entregable_cambio_descripcion (
    id BIGSERIAL PRIMARY KEY,
    entregable_id INTEGER NOT NULL REFERENCES entregable(entregable_id) ON DELETE CASCADE,
    descripcion_anterior TEXT,
    descripcion_nueva TEXT NOT NULL,
    justificacion TEXT NOT NULL,
    archivo_pdf TEXT NOT NULL,
    nombre_original VARCHAR(300),
    usuario VARCHAR(255) NOT NULL,
    usuario_rol VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cambio_descripcion_entregable
    ON entregable_cambio_descripcion(entregable_id);

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('ENTREGABLE:CAMBIAR_DESCRIPCION', 'Cambiar descripcion de entregable', 'Permite modificar la descripcion de un entregable existente con justificacion y soporte')
ON CONFLICT (codigo) DO UPDATE
SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    activo = TRUE;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo = 'ENTREGABLE:CAMBIAR_DESCRIPCION'
WHERE r.codigo IN ('admin', 'gestor_tic', 'gestor_proyectos')
ON CONFLICT (rol_id, permiso_id) DO UPDATE
SET activo = TRUE;
