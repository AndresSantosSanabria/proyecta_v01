ALTER TABLE proyecta_db.proyecto
    ADD COLUMN IF NOT EXISTS requiere_completitud_director BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS primer_ingreso_director_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS completado_por_director_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS registrado_inicial_por VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS alcance_detallado TEXT NULL,
    ADD COLUMN IF NOT EXISTS presupuesto_estimado NUMERIC(18, 2) NULL;

ALTER TABLE proyecta_db.proyecto DROP CONSTRAINT IF EXISTS proyecto_estado_check;
ALTER TABLE proyecta_db.proyecto ADD CONSTRAINT proyecto_estado_check
    CHECK (estado IN (
        'PENDIENTE_COMPLETAR',
        'PLANIFICACION',
        'ACTIVO',
        'CON_RETRASOS',
        'CERRADO',
        'EN_REVISION',
        'FINALIZADO'
    ));

INSERT INTO proyecta_db.estado_proyecto_config
    (codigo, nombre, descripcion, color_hex, es_terminal, orden, activo)
VALUES
    ('PENDIENTE_COMPLETAR', 'Pendiente de Completar', 'Proyecto registrado por gestor y pendiente de completitud por el Director asignado.', '#f59e0b', false, 0, true),
    ('PLANIFICACION', 'Planificacion', 'Proyecto completado por el Director y habilitado para planificacion operativa.', '#3b82f6', false, 1, true)
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    color_hex = EXCLUDED.color_hex,
    es_terminal = EXCLUDED.es_terminal,
    orden = EXCLUDED.orden,
    activo = EXCLUDED.activo;
