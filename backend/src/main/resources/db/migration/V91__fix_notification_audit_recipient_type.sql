-- Corrige el tipo de la columna `recipient` de la tabla `notification_audit`.
-- En algunas bases el esquema quedó con la columna como bytea, lo que impide
-- usar lower() / LIKE en el filtrado por destinatario.
-- Se convierte a VARCHAR(255), el tipo que define la migración original V34.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'notification_audit'
          AND column_name = 'recipient'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.notification_audit
            ALTER COLUMN recipient TYPE VARCHAR(255)
            USING convert_from(recipient, 'UTF8');
    END IF;
END $$;
