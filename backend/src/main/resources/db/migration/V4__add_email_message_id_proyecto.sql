-- V3: Agrega columna para hilo de correo por proyecto (Outlook threading)
ALTER TABLE proyecta_db.proyecto
    ADD COLUMN IF NOT EXISTS email_message_id VARCHAR(255);
