-- Agregar campos para borrador de completitud por fases
-- Permite a los Directores guardar progreso parcial en el wizard de completitud

ALTER TABLE proyecto 
ADD COLUMN completitud_borrador_json TEXT,
ADD COLUMN completitud_fases_completadas TEXT;

-- Comentarios de las columnas
COMMENT ON COLUMN proyecto.completitud_borrador_json IS 'JSON con el borrador de completitud del proyecto por fases. Almacena los datos ingresados por el Director mientras completa la información inicial.';
COMMENT ON COLUMN proyecto.completitud_fases_completadas IS 'JSON con el estado de cada fase de completitud (1-7). Ejemplo: {"1":true,"2":true,"3":false,...}';
