-- ================================================================
-- SCRIPT DE MIGRACIÓN Y CORRECCIÓN DE PONDERACIONES
-- Base de Datos: proyecta_db
-- Propósito: Auditar y corregir inconsistencias en ponderaciones
-- ================================================================

-- 1. FUNCIÓN PARA CALCULAR SUMA DE PONDERACIONES POR PROYECTO
CREATE OR REPLACE FUNCTION calc_suma_ponderaciones_fases(p_proyecto_id VARCHAR)
RETURNS NUMERIC AS $$
BEGIN
    RETURN COALESCE(
        (SELECT SUM(ponderacion) FROM fase WHERE proyecto_id = p_proyecto_id),
        0
    );
END;
$$ LANGUAGE plpgsql;

-- 2. FUNCIÓN PARA NORMALIZAR PONDERACIONES DE FASES
CREATE OR REPLACE FUNCTION normalizar_ponderaciones_fases(p_proyecto_id VARCHAR)
RETURNS TABLE(fase_id INTEGER, nombre VARCHAR, ponderacion_antigua NUMERIC, ponderacion_nueva NUMERIC) AS $$
DECLARE
    v_suma NUMERIC;
    v_factor NUMERIC;
    v_diferencia NUMERIC;
    v_ultima_fase_id INTEGER;
BEGIN
    -- Calcular suma actual
    v_suma := calc_suma_ponderaciones_fases(p_proyecto_id);

    -- Si suma es 0, no se puede normalizar
    IF v_suma <= 0 THEN
        RETURN;
    END IF;

    -- Si suma es 100, no hay que normalizar
    IF v_suma = 100 THEN
        RETURN;
    END IF;

    -- Calcular factor de escala
    v_factor := 100.0 / v_suma;

    -- Aplicar factor a todas las fases
    CREATE TEMP TABLE temp_fases_normalizadas AS
    SELECT 
        f.fase_id,
        f.nombre,
        f.ponderacion,
        ROUND((f.ponderacion * v_factor)::NUMERIC, 2) AS ponderacion_normalizada,
        ROW_NUMBER() OVER (ORDER BY f.fase_id DESC) AS rn
    FROM fase f
    WHERE f.proyecto_id = p_proyecto_id
    ORDER BY f.fase_id;

    -- Actualizar ponderaciones
    UPDATE fase f
    SET ponderacion = tfn.ponderacion_normalizada
    FROM temp_fases_normalizadas tfn
    WHERE f.fase_id = tfn.fase_id;

    -- Ajuste de redondeo: si suma aún no es exacto 100, ajustar la última fase
    v_suma := calc_suma_ponderaciones_fases(p_proyecto_id);
    IF v_suma != 100 THEN
        v_diferencia := 100 - v_suma;
        SELECT fase_id INTO v_ultima_fase_id 
        FROM fase 
        WHERE proyecto_id = p_proyecto_id 
        ORDER BY fase_id DESC 
        LIMIT 1;
        
        IF v_ultima_fase_id IS NOT NULL THEN
            UPDATE fase 
            SET ponderacion = ponderacion + v_diferencia 
            WHERE fase_id = v_ultima_fase_id;
        END IF;
    END IF;

    -- Retornar reporte
    RETURN QUERY
    SELECT 
        tfn.fase_id,
        tfn.nombre,
        tfn.ponderacion,
        COALESCE(f.ponderacion, tfn.ponderacion_normalizada)
    FROM temp_fases_normalizadas tfn
    LEFT JOIN fase f ON tfn.fase_id = f.fase_id;

    DROP TABLE temp_fases_normalizadas;
END;
$$ LANGUAGE plpgsql;

-- ================================================================
-- AUDITORÍA: PROYECTOS CON PONDERACIONES INCONSISTENTES
-- ================================================================

-- 3. VER PROYECTOS CON PONDERACIONES INCONSISTENTES
SELECT 
    p.proyecto_id,
    p.nombre,
    COUNT(DISTINCT f.fase_id) AS cantidad_fases,
    COALESCE(SUM(f.ponderacion), 0) AS suma_ponderaciones,
    CASE 
        WHEN SUM(f.ponderacion) = 100 THEN '✓ Consistente'
        WHEN SUM(f.ponderacion) < 100 THEN '⚠ Incompleta (necesita normalización)'
        WHEN SUM(f.ponderacion) > 100 THEN '✗ Excedida (ERROR - requiere corrección manual)'
    END AS estado
FROM proyecto p
LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
GROUP BY p.proyecto_id, p.nombre
HAVING SUM(f.ponderacion) IS NULL OR SUM(f.ponderacion) != 100
ORDER BY suma_ponderaciones DESC;

-- ================================================================
-- NORMALIZACIÓN AUTOMÁTICA
-- ================================================================

-- 4. APLICAR NORMALIZACIÓN A TODOS LOS PROYECTOS INCONSISTENTES
DO $$
DECLARE
    v_proyecto RECORD;
    v_resultado RECORD;
BEGIN
    -- Obtener todos los proyectos con ponderaciones inconsistentes
    FOR v_proyecto IN 
        SELECT DISTINCT p.proyecto_id
        FROM proyecto p
        LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
        GROUP BY p.proyecto_id
        HAVING SUM(f.ponderacion) IS NULL OR SUM(f.ponderacion) != 100
    LOOP
        RAISE NOTICE 'Normalizando ponderaciones del proyecto: %', v_proyecto.proyecto_id;
        
        -- Aplicar normalización
        FOR v_resultado IN SELECT * FROM normalizar_ponderaciones_fases(v_proyecto.proyecto_id) LOOP
            RAISE NOTICE '  Fase % (%) - Anterior: %% → Nueva: %%', 
                v_resultado.fase_id, 
                v_resultado.nombre,
                v_resultado.ponderacion_antigua,
                v_resultado.ponderacion_nueva;
        END LOOP;
    END LOOP;
END $$;

-- 5. VERIFICAR CONSISTENCIA DESPUÉS DE LA NORMALIZACIÓN
SELECT 
    p.proyecto_id,
    p.nombre,
    COUNT(DISTINCT f.fase_id) AS cantidad_fases,
    COALESCE(SUM(f.ponderacion), 0) AS suma_ponderaciones,
    CASE 
        WHEN SUM(f.ponderacion) = 100 THEN '✓ Consistente'
        ELSE '✗ Aún inconsistente'
    END AS estado
FROM proyecto p
LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
GROUP BY p.proyecto_id, p.nombre
ORDER BY suma_ponderaciones DESC;

-- ================================================================
-- TRIGGERS PARA PREVENIR FUTURAS INCONSISTENCIAS
-- ================================================================

-- 6. TRIGGER: Validar ponderación al insertar fase
CREATE OR REPLACE FUNCTION tr_validar_ponderacion_fase_insert()
RETURNS TRIGGER AS $$
DECLARE
    v_suma NUMERIC;
BEGIN
    -- Validar que ponderación sea válida (0.01 a 100)
    IF NEW.ponderacion < 0.01 OR NEW.ponderacion > 100 THEN
        RAISE EXCEPTION 'Ponderación % no está en rango válido (0.01 a 100)', NEW.ponderacion;
    END IF;

    -- Calcular suma con la nueva fase
    v_suma := (SELECT COALESCE(SUM(ponderacion), 0) FROM fase WHERE proyecto_id = NEW.proyecto_id);
    v_suma := v_suma + NEW.ponderacion;

    -- Lanzar advertencia si supera 100
    IF v_suma > 100 THEN
        RAISE WARNING 'Ponderaciones del proyecto % sumarán %.2f%% (límite: 100%%)', 
            NEW.proyecto_id, v_suma;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_validar_ponderacion_fase_insert ON fase;
CREATE TRIGGER tr_validar_ponderacion_fase_insert
BEFORE INSERT ON fase
FOR EACH ROW
EXECUTE FUNCTION tr_validar_ponderacion_fase_insert();

-- 7. TRIGGER: Validar ponderación al actualizar fase
CREATE OR REPLACE FUNCTION tr_validar_ponderacion_fase_update()
RETURNS TRIGGER AS $$
DECLARE
    v_suma NUMERIC;
    v_diferencia NUMERIC;
BEGIN
    -- Validar que ponderación sea válida (0.01 a 100)
    IF NEW.ponderacion < 0.01 OR NEW.ponderacion > 100 THEN
        RAISE EXCEPTION 'Ponderación % no está en rango válido (0.01 a 100)', NEW.ponderacion;
    END IF;

    -- Calcular suma reemplazando el valor anterior
    v_diferencia := NEW.ponderacion - OLD.ponderacion;
    v_suma := (SELECT COALESCE(SUM(ponderacion), 0) FROM fase WHERE proyecto_id = NEW.proyecto_id);
    v_suma := v_suma + v_diferencia;

    -- Lanzar advertencia si supera 100
    IF v_suma > 100 THEN
        RAISE WARNING 'Ponderaciones del proyecto % sumarán %.2f%% (límite: 100%%)', 
            NEW.proyecto_id, v_suma;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_validar_ponderacion_fase_update ON fase;
CREATE TRIGGER tr_validar_ponderacion_fase_update
BEFORE UPDATE ON fase
FOR EACH ROW
WHEN (OLD.ponderacion IS DISTINCT FROM NEW.ponderacion)
EXECUTE FUNCTION tr_validar_ponderacion_fase_update();

-- ================================================================
-- AUDITORÍA: TABLA DE HISTORIAL DE CAMBIOS
-- ================================================================

-- 8. CREAR TABLA DE AUDITORÍA DE PONDERACIONES
CREATE TABLE IF NOT EXISTS auditoria_ponderaciones (
    auditoria_id SERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30),
    fase_id INTEGER,
    ponderacion_anterior NUMERIC(5,2),
    ponderacion_nueva NUMERIC(5,2),
    razon_cambio VARCHAR(255),
    usuario_id VARCHAR(50),
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (proyecto_id) REFERENCES proyecto(proyecto_id),
    FOREIGN KEY (fase_id) REFERENCES fase(fase_id)
);

CREATE INDEX idx_auditoria_ponderaciones_proyecto ON auditoria_ponderaciones(proyecto_id);
CREATE INDEX idx_auditoria_ponderaciones_fecha ON auditoria_ponderaciones(fecha_cambio DESC);

-- 9. TRIGGER: Registrar cambios en ponderaciones
CREATE OR REPLACE FUNCTION tr_auditar_cambios_ponderacion()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.ponderacion IS DISTINCT FROM NEW.ponderacion THEN
        INSERT INTO auditoria_ponderaciones 
        (proyecto_id, fase_id, ponderacion_anterior, ponderacion_nueva, razon_cambio, usuario_id)
        VALUES 
        (NEW.proyecto_id, NEW.fase_id, OLD.ponderacion, NEW.ponderacion, 
         'Actualización manual', CURRENT_USER);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_auditar_cambios_ponderacion ON fase;
CREATE TRIGGER tr_auditar_cambios_ponderacion
AFTER UPDATE ON fase
FOR EACH ROW
EXECUTE FUNCTION tr_auditar_cambios_ponderacion();

-- ================================================================
-- REPORTE FINAL
-- ================================================================

-- 10. REPORTE DE AUDITORÍA
SELECT 
    '📊 REPORTE DE PONDERACIONES' AS reporte,
    'Total de proyectos' AS metrica,
    COUNT(DISTINCT p.proyecto_id)::TEXT AS valor
FROM proyecto p
UNION ALL
SELECT 
    '📊 REPORTE DE PONDERACIONES',
    'Proyectos consistentes',
    COUNT(DISTINCT p.proyecto_id)::TEXT
FROM proyecto p
LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
GROUP BY p.proyecto_id
HAVING SUM(f.ponderacion) = 100 OR SUM(f.ponderacion) IS NULL
UNION ALL
SELECT 
    '📊 REPORTE DE PONDERACIONES',
    'Proyectos que necesitan normalización',
    COUNT(DISTINCT p.proyecto_id)::TEXT
FROM proyecto p
LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
GROUP BY p.proyecto_id
HAVING SUM(f.ponderacion) < 100 AND SUM(f.ponderacion) > 0
UNION ALL
SELECT 
    '📊 REPORTE DE PONDERACIONES',
    'Proyectos con error (suma > 100%)',
    COUNT(DISTINCT p.proyecto_id)::TEXT
FROM proyecto p
LEFT JOIN fase f ON p.proyecto_id = f.proyecto_id
GROUP BY p.proyecto_id
HAVING SUM(f.ponderacion) > 100;
