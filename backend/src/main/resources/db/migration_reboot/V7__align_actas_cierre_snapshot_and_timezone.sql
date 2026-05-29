-- V7: Alineacion de actas_cierre con la entidad de cierre y snapshot analitico
-- Fecha: 2026-05-28

ALTER TABLE proyecta_db.actas_cierre
    ADD COLUMN IF NOT EXISTS progreso_programado_final NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS progreso_ejecutado_final NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS diferencia_final NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS eficacia_final NUMERIC(6, 4),
    ADD COLUMN IF NOT EXISTS estado_final VARCHAR(30),
    ADD COLUMN IF NOT EXISTS corte_calculo DATE,
    ADD COLUMN IF NOT EXISTS snapshot_json TEXT;

ALTER TABLE proyecta_db.actas_cierre
    ALTER COLUMN fecha_cierre TYPE TIMESTAMPTZ
    USING fecha_cierre AT TIME ZONE 'America/Bogota';

COMMENT ON COLUMN proyecta_db.actas_cierre.corte_calculo IS 'Fecha de corte institucional usada para consolidar la instantanea del cierre.';
COMMENT ON COLUMN proyecta_db.actas_cierre.snapshot_json IS 'Snapshot inmutable de la analitica consolidada al cierre tecnico.';
