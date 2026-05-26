ALTER TABLE IF EXISTS proyecta_db.actas_cierre
    ADD COLUMN IF NOT EXISTS progreso_programado_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS progreso_ejecutado_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS diferencia_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS eficacia_final NUMERIC(6,4),
    ADD COLUMN IF NOT EXISTS estado_final VARCHAR(30),
    ADD COLUMN IF NOT EXISTS corte_calculo DATE,
    ADD COLUMN IF NOT EXISTS snapshot_json TEXT;
