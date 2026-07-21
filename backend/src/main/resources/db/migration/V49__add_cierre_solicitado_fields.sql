ALTER TABLE proyecto ADD COLUMN cierre_solicitado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyecto ADD COLUMN cierre_solicitado_en TIMESTAMP NULL;
ALTER TABLE proyecto ADD COLUMN cierre_solicitado_por VARCHAR(120) NULL;
