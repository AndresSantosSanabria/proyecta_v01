ALTER TABLE IF EXISTS proyecta_db.actas_cierre
    ADD COLUMN IF NOT EXISTS archivo_pdf VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ruta_archivo_pdf VARCHAR(255);
