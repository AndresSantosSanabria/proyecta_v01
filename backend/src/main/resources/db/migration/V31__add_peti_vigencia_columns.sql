-- Agrega las columnas vigencia_desde y vigencia_hasta a la tabla estrategia_peti_config
-- que fueron definidas en V1__init pero pueden faltar si la primera version de la tabla
-- (sin prefijo proyecta_db) se creo primero.

ALTER TABLE proyecta_db.estrategia_peti_config
    ADD COLUMN IF NOT EXISTS vigencia_desde VARCHAR(20);

ALTER TABLE proyecta_db.estrategia_peti_config
    ADD COLUMN IF NOT EXISTS vigencia_hasta VARCHAR(20);
