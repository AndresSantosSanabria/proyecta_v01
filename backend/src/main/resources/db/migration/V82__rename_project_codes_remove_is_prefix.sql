-- Migracion V82: Renombrar codigos de proyecto de IS-PROY-CUN- a PROY-CUN-
-- Primero elimina duplicados parciales de V81, luego renombra correctamente
-- Usa DO block unico para manejar FK constraints en una sola transaccion

DO $$
DECLARE
    r RECORD;
    new_def TEXT;
    tbl RECORD;
BEGIN
    -- 0. Eliminar registros con PROY-CUN-% en tablas sin CASCADE (bloquearian DELETE)
    DELETE FROM proyecta_db.actas_cierre WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.documento_dinamico WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.auditoria_ponderaciones WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.project_closures WHERE proyecto_id LIKE 'PROY-CUN-%';

    -- 1. Eliminar proyectos duplicados PROY-CUN-% (CASCADE elimina hijos)
    DELETE FROM proyecta_db.proyecto WHERE proyecto_id LIKE 'PROY-CUN-%';

    -- 2. Eliminar registros huérfanos restantes en tablas sin CASCADE
    DELETE FROM proyecta_db.documento_proyecto_version WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.proyecto_stakeholder WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.proyecto_beneficio_impacto WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.usuario_proyecto WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.documento WHERE proyecto_id LIKE 'PROY-CUN-%';
    DELETE FROM proyecta_db.respuestas_furag WHERE proyecto_id LIKE 'PROY-CUN-%';

    -- 3. Guardar y dropear FK constraints que referencian proyecto
    CREATE TEMPORARY TABLE _v82_fk_backup (
        id SERIAL PRIMARY KEY,
        table_schema TEXT,
        table_name TEXT,
        constraint_name TEXT,
        constraint_def TEXT
    );

    INSERT INTO _v82_fk_backup (table_schema, table_name, constraint_name, constraint_def)
    SELECT
        n.nspname,
        c.relname,
        con.conname,
        pg_get_constraintdef(con.oid)
    FROM pg_constraint con
    JOIN pg_class c ON con.conrelid = c.oid
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE con.confrelid = 'proyecta_db.proyecto'::regclass
      AND con.contype = 'f';

    FOR r IN SELECT * FROM _v82_fk_backup LOOP
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I', r.table_schema, r.table_name, r.constraint_name);
    END LOOP;

    -- 4. Renombrar tabla PROYECTO (tabla padre)
    UPDATE proyecta_db.proyecto
    SET proyecto_id = regexp_replace(proyecto_id, '^IS-', '')
    WHERE proyecto_id LIKE 'IS-PROY-CUN-%';

    -- 5. Renombrar TODAS las tablas hijas que tienen proyecto_id directo
    FOR tbl IN
        SELECT unnest(ARRAY[
            'fase', 'riesgos', 'objetivos_especificos', 'proyecto_equipo',
            'respuestas_furag', 'documento', 'documento_dinamico',
            'usuario_proyecto', 'proyecto_beneficio_impacto',
            'documento_proyecto_version', 'proyecto_stakeholder',
            'auditoria_ponderaciones', 'actas_cierre', 'project_closures'
        ]) AS table_name
    LOOP
        EXECUTE format(
            'UPDATE proyecta_db.%I SET proyecto_id = regexp_replace(proyecto_id, %L, %L) WHERE proyecto_id LIKE %L',
            tbl.table_name, '^IS-', '', 'IS-PROY-CUN-%'
        );
    END LOOP;

    -- 6. Recrear FK constraints con schema-qualification corregida
    FOR r IN SELECT * FROM _v82_fk_backup LOOP
        new_def := r.constraint_def;
        new_def := replace(new_def, 'REFERENCES proyecto(', format('REFERENCES %I.proyecto(', r.table_schema));
        new_def := replace(new_def, 'REFERENCES proyecta_db.proyecto(', format('REFERENCES %I.proyecto(', r.table_schema));
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',
            r.table_schema, r.table_name, r.constraint_name, new_def);
    END LOOP;

    DROP TABLE IF EXISTS _v82_fk_backup;
END $$;
