-- ========================================================
-- Corrige el tipo de las columnas de la tabla system_audit_log.
-- En algunas bases el esquema quedó con columnas como bytea, lo que
-- impide usar lower() / LIKE en los filtros de la consulta de auditoría.
-- Se convierten a los tipos que define la migración original V92.
-- ========================================================

DO $$
BEGIN
    -- Conversión de columnas de texto que pueden haber quedado como bytea.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'usuario_id'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN usuario_id TYPE VARCHAR(100) USING convert_from(usuario_id, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'usuario_nombre'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN usuario_nombre TYPE VARCHAR(255) USING convert_from(usuario_nombre, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'usuario_rol'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN usuario_rol TYPE VARCHAR(100) USING convert_from(usuario_rol, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'accion'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN accion TYPE VARCHAR(50) USING convert_from(accion, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'modulo'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN modulo TYPE VARCHAR(150) USING convert_from(modulo, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'metodo_http'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN metodo_http TYPE VARCHAR(10) USING convert_from(metodo_http, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'recurso'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN recurso TYPE VARCHAR(255) USING convert_from(recurso, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'estado'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN estado TYPE VARCHAR(20) USING convert_from(estado, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'detalle'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN detalle TYPE TEXT USING convert_from(detalle, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'traza_error'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN traza_error TYPE TEXT USING convert_from(traza_error, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'ip_origen'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN ip_origen TYPE VARCHAR(45) USING convert_from(ip_origen, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'proyecta_db'
          AND table_name = 'system_audit_log'
          AND column_name = 'user_agent'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE proyecta_db.system_audit_log
            ALTER COLUMN user_agent TYPE TEXT USING convert_from(user_agent, 'UTF8');
    END IF;
END $$;
