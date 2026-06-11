ALTER TABLE IF EXISTS proyecta_db.entregable
    DROP CONSTRAINT IF EXISTS entregable_estado_check;

ALTER TABLE IF EXISTS proyecta_db.entregable
    ADD CONSTRAINT entregable_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'RECHAZADO', 'A_CONFORMIDAD', 'COMPLETADO', 'ATRASADO'));

ALTER TABLE IF EXISTS proyecta_db.entregable
    ADD COLUMN IF NOT EXISTS observacion_revision VARCHAR(1000);

INSERT INTO proyecta_db.estado_entregable_config
    (codigo, nombre, descripcion, es_conforme, es_terminal, cuenta_avance, color_hex, orden, activo)
SELECT
    'RECHAZADO',
    'Rechazado',
    'Entregable revisado y devuelto para correcciones',
    FALSE,
    FALSE,
    0,
    '#ef4444',
    3,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM proyecta_db.estado_entregable_config
    WHERE codigo = 'RECHAZADO'
);
