ALTER TABLE proyecta_db.lista_parametrica_config ADD COLUMN IF NOT EXISTS lista_nombre_campo VARCHAR(200) NULL;
ALTER TABLE proyecta_db.lista_parametrica_config ADD COLUMN IF NOT EXISTS lista_descripcion VARCHAR(500) NULL;
ALTER TABLE proyecta_db.lista_parametrica_config ADD COLUMN IF NOT EXISTS lista_tipo VARCHAR(30) DEFAULT 'Lista';
