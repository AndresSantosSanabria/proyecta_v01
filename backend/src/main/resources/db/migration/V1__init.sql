
-- MIGRATION: V1__create_documento_table.sql --

-- Migration: Create documento table
-- Date: 2026-05-20
-- Description: Tabla para registrar metadatos de documentos cargados al sistema

CREATE TABLE IF NOT EXISTS proyecta_db.documento (
    id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    tipo_documento VARCHAR(30) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    url_descarga VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_documento_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    CONSTRAINT chk_tipo_documento CHECK (tipo_documento IN ('VIABILIZACION', 'ACTA_CONSTITUCION', 'CRONOGRAMA', 'PLAN_COMUNICACIONES')),
    CONSTRAINT chk_tamano_positivo CHECK (tamano_bytes > 0)
);

CREATE INDEX IF NOT EXISTS idx_documento_proyecto ON proyecta_db.documento(proyecto_id);
CREATE INDEX IF NOT EXISTS idx_documento_tipo ON proyecta_db.documento(tipo_documento);
CREATE INDEX IF NOT EXISTS idx_documento_proyecto_tipo ON proyecta_db.documento(proyecto_id, tipo_documento);

COMMENT ON TABLE proyecta_db.documento IS 'Registro de metadatos de documentos cargados por proyecto';
COMMENT ON COLUMN proyecta_db.documento.nombre_original IS 'Nombre original del archivo subido por el usuario';
COMMENT ON COLUMN proyecta_db.documento.nombre_almacenado IS 'Nombre único generado con UUID para evitar colisiones';
COMMENT ON COLUMN proyecta_db.documento.ruta_almacenamiento IS 'Subdirectorio relativo dentro del storage (ej: documentos)';
COMMENT ON COLUMN proyecta_db.documento.mime_type IS 'Tipo MIME real detectado en el backend';
COMMENT ON COLUMN proyecta_db.documento.tamano_bytes IS 'Tamaño del archivo en bytes';



-- MIGRATION: V2__create_documento_dinamico_table.sql --

CREATE TABLE documento_dinamico (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    proyecto_id VARCHAR(30) NOT NULL,
    tipo_documento VARCHAR(100) NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL UNIQUE,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    url_descarga VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    fecha_carga TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT idx_proyecto_tipo UNIQUE (proyecto_id, tipo_documento),
    FOREIGN KEY (proyecto_id) REFERENCES proyecto(proyecto_id)
);

CREATE INDEX idx_documento_dinamico_proyecto_tipo ON documento_dinamico(proyecto_id, tipo_documento);

ALTER TABLE entregable DROP CONSTRAINT IF EXISTS entregable_estado_check;
ALTER TABLE entregable ADD CONSTRAINT entregable_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'A_CONFORMIDAD', 'COMPLETADO', 'ATRASADO'));



-- MIGRATION: V3__recalculate_all_advances.sql --

-- Migration: Recalculate all project advances based on current deliverable states
-- Date: 2026-05-20
-- Description: Updates avance_total for all projects based on COMPLETADO/A_CONFORMIDAD deliverables

-- Recalculate hito advances
UPDATE proyecta_db.hito h
SET avance_calculado = (
    SELECT COALESCE(SUM(e.ponderacion), 0)
    FROM proyecta_db.entregable e
    WHERE e.hito_id = h.hito_id
    AND e.estado IN ('COMPLETADO', 'A_CONFORMIDAD')
);

-- Recalculate fase advances
UPDATE proyecta_db.fase f
SET avance_calculado = (
    SELECT COALESCE(SUM(h.avance_calculado * h.ponderacion / 100), 0)
    FROM proyecta_db.hito h
    WHERE h.fase_id = f.fase_id
);

-- Recalculate project advances
UPDATE proyecta_db.proyecto p
SET avance_total = (
    SELECT COALESCE(SUM(f.avance_calculado * f.ponderacion / 100), 0)
    FROM proyecta_db.fase f
    WHERE f.proyecto_id = p.proyecto_id
);



-- MIGRATION: V4__fix_enum_db_mismatches.sql --

-- Migration V4: Fix critical enum/DB mismatches
-- Date: 2026-05-20
-- Description: Fixes mismatches between Java enums and PostgreSQL CHECK constraints

-- Fix 1: EstadoProyecto - expand CHECK to include all enum values
ALTER TABLE proyecta_db.proyecto DROP CONSTRAINT IF EXISTS proyecto_estado_check;
ALTER TABLE proyecta_db.proyecto ADD CONSTRAINT proyecto_estado_check
    CHECK (estado IN ('ACTIVO', 'CON_RETRASOS', 'CERRADO', 'EN_REVISION', 'FINALIZADO'));

-- Fix 2: Rol - reconcile enum and DB values
ALTER TABLE proyecta_db.usuario DROP CONSTRAINT IF EXISTS usuario_rol_check;
ALTER TABLE proyecta_db.usuario ADD CONSTRAINT usuario_rol_check
    CHECK (rol IN ('ADMINISTRADOR', 'GESTOR_PROYECTOS_TI', 'GESTOR_PROYECTOS', 'ANALISTA_PROYECTOS'));

-- Fix 3 & 4: Probabilidad/Impacto - change column types from INTEGER to VARCHAR
ALTER TABLE proyecta_db.riesgos ALTER COLUMN probabilidad TYPE VARCHAR(10);
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_probabilidad_check;
ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_probabilidad_check
    CHECK (probabilidad IN ('BAJA', 'MEDIA', 'ALTA'));

ALTER TABLE proyecta_db.riesgos ALTER COLUMN impacto TYPE VARCHAR(10);
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_impacto_check;
ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_impacto_check
    CHECK (impacto IN ('BAJO', 'MEDIO', 'ALTO'));

-- Fix 5: EstadoRiesgo - align case (uppercase)
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_estado_check;
ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_estado_check
    CHECK (estado IN ('PENDIENTE', 'TRATADO'));

-- Fix existing data: convert any Title-case estado values to uppercase
UPDATE proyecta_db.riesgos SET estado = 'PENDIENTE' WHERE estado = 'Pendiente';
UPDATE proyecta_db.riesgos SET estado = 'TRATADO' WHERE estado = 'Tratado';



-- MIGRATION: V5__create_config_tables.sql --

-- Migration V5: Create configuration tables for normalized enums
-- Date: 2026-05-20
-- Description: Creates lookup tables for business configuration that was previously hardcoded as enums

-- 1. Tipo Documento Config
CREATE TABLE IF NOT EXISTS proyecta_db.tipo_documento_config (
    tipo_documento_id   SERIAL          PRIMARY KEY,
    codigo              VARCHAR(30)     NOT NULL UNIQUE,
    nombre              VARCHAR(100)    NOT NULL,
    descripcion         VARCHAR(300),
    requiere_pdf        BOOLEAN         NOT NULL DEFAULT TRUE,
    orden               INTEGER         NOT NULL DEFAULT 0,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

INSERT INTO proyecta_db.tipo_documento_config (codigo, nombre, descripcion, orden, activo) VALUES
('VIABILIZACION',          'Viabilizacion',          'Documento de viabilidad del proyecto',          1, true),
('ACTA_CONSTITUCION',      'Acta de Constitucion',   'Acta formal de constitucion del proyecto',      2, true),
('CRONOGRAMA',             'Cronograma',             'Plan de cronograma del proyecto',               3, true),
('PLAN_COMUNICACIONES',    'Plan de Comunicaciones', 'Plan de comunicaciones del proyecto',           4, true);

-- 2. Estado Proyecto Config
CREATE TABLE IF NOT EXISTS proyecta_db.estado_proyecto_config (
    estado_proyecto_id  SERIAL          PRIMARY KEY,
    codigo              VARCHAR(20)     NOT NULL UNIQUE,
    nombre              VARCHAR(50)     NOT NULL,
    descripcion         VARCHAR(300),
    color_hex           VARCHAR(7),
    es_terminal         BOOLEAN         NOT NULL DEFAULT FALSE,
    orden               INTEGER         NOT NULL DEFAULT 0,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.estado_proyecto_config (codigo, nombre, descripcion, color_hex, es_terminal, orden, activo) VALUES
('ACTIVO',        'Activo',        'Proyecto en ejecucion normal',        '#22c55e', false, 1, true),
('CON_RETRASOS',  'Con Retrasos',  'Proyecto con entregables vencidos',   '#f59e0b', false, 2, true),
('EN_REVISION',   'En Revision',   'Proyecto en proceso de revision',     '#3b82f6', false, 3, true),
('FINALIZADO',    'Finalizado',    'Proyecto completado sin acta formal', '#8b5cf6', true,  4, true),
('CERRADO',       'Cerrado',       'Proyecto cerrado formalmente',        '#6b7280', true,  5, true);

-- 3. Estado Entregable Config
CREATE TABLE IF NOT EXISTS proyecta_db.estado_entregable_config (
    estado_entregable_id  SERIAL          PRIMARY KEY,
    codigo                VARCHAR(20)     NOT NULL UNIQUE,
    nombre                VARCHAR(50)     NOT NULL,
    descripcion           VARCHAR(300),
    es_conforme           BOOLEAN         NOT NULL DEFAULT FALSE,
    es_terminal           BOOLEAN         NOT NULL DEFAULT FALSE,
    cuenta_avance         NUMERIC(5,2)    NOT NULL DEFAULT 0,
    color_hex             VARCHAR(7),
    orden                 INTEGER         NOT NULL DEFAULT 0,
    activo                BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.estado_entregable_config (codigo, nombre, descripcion, es_conforme, es_terminal, cuenta_avance, color_hex, orden, activo) VALUES
('PENDIENTE',     'Pendiente',     'Entregable no iniciado',              false, false, 0,    '#9ca3af', 1, true),
('EN_PROCESO',    'En Proceso',    'Entregable en elaboracion',           false, false, 0,    '#3b82f6', 2, true),
('A_CONFORMIDAD', 'A Conformidad', 'Entregable aprobado formalmente',     true,  true,  100,  '#22c55e', 3, true),
('COMPLETADO',    'Completado',    'Entregable entregado con evidencia',  true,  true,  100,  '#10b981', 4, true),
('ATRASADO',      'Atrasado',      'Entregable vencido sin entregar',     false, false, 0,    '#ef4444', 5, true);

-- 4. Estrategia PETI Config
CREATE TABLE IF NOT EXISTS proyecta_db.estrategia_peti_config (
    estrategia_peti_id  SERIAL          PRIMARY KEY,
    codigo              VARCHAR(80)     NOT NULL UNIQUE,
    nombre              VARCHAR(150)    NOT NULL,
    descripcion         TEXT,
    vigencia_desde      VARCHAR(20),
    vigencia_hasta      VARCHAR(20),
    orden               INTEGER         NOT NULL DEFAULT 0,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.estrategia_peti_config (codigo, nombre, descripcion, orden, activo) VALUES
('TECNOLOGIAS_INFORMACION',            'Tecnologias de la Informacion',           'Estrategia de TI institucional',              1, true),
('TRANSFORMACION_DIGITAL',             'Transformacion Digital',                  'Estrategia de transformacion digital',        2, true),
('CIUDADES_TERRITORIOS_INTELIGENTES',  'Ciudades y Territorios Inteligentes',     'Estrategia de ciudades inteligentes',         3, true),
('GOBIERNO_DIGITAL',                   'Gobierno Digital',                        'Estrategia de gobierno digital',              4, true);

-- 5. Rol Config
CREATE TABLE IF NOT EXISTS proyecta_db.rol_config (
    rol_id              SERIAL          PRIMARY KEY,
    codigo              VARCHAR(30)     NOT NULL UNIQUE,
    nombre              VARCHAR(100)    NOT NULL,
    descripcion         VARCHAR(300),
    nivel_acceso        INTEGER         NOT NULL DEFAULT 0,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.rol_config (codigo, nombre, descripcion, nivel_acceso, activo) VALUES
('ADMINISTRADOR',         'Administrador',          'Acceso total al sistema',              100, true),
('GESTOR_PROYECTOS_TI',   'Gestor de Proyectos TI', 'Gestion de proyectos tecnologicos',    80,  true),
('GESTOR_PROYECTOS',      'Gestor de Proyectos',    'Gestion general de proyectos',         60,  true),
('ANALISTA_PROYECTOS',    'Analista de Proyectos',  'Analisis y seguimiento',               40,  true);

WITH admin_role AS (
    SELECT rol_id
    FROM proyecta_db.rol_config
    WHERE codigo = 'ADMINISTRADOR'
    LIMIT 1
)
INSERT INTO proyecta_db.usuario (
    keycloak_sub,
    nombre,
    correo,
    contrasena_hash,
    rol,
    rol_config_id,
    activo
)
SELECT
    'fabio.santos@cundinamarca.gov.co',
    'Fabio Santos',
    'fabio.santos@cundinamarca.gov.co',
    'hash_simulado',
    'ADMINISTRADOR',
    admin_role.rol_id,
    TRUE
FROM admin_role
ON CONFLICT (correo) DO UPDATE
SET
    keycloak_sub = EXCLUDED.keycloak_sub,
    nombre = EXCLUDED.nombre,
    contrasena_hash = EXCLUDED.contrasena_hash,
    rol = EXCLUDED.rol,
    rol_config_id = EXCLUDED.rol_config_id,
    activo = TRUE;

COMMENT ON COLUMN proyecta_db.usuario.rol_config_id IS
    'Rol local asociado. El usuario fabio.santos@cundinamarca.gov.co debe quedar como ADMINISTRADOR.';

-- 6. Matriz de Riesgo (Probabilidad x Impacto -> Nivel)
CREATE TABLE IF NOT EXISTS proyecta_db.matriz_riesgo (
    matriz_riesgo_id    SERIAL          PRIMARY KEY,
    probabilidad        VARCHAR(30)     NOT NULL,
    impacto             VARCHAR(30)     NOT NULL,
    nivel_resultante    VARCHAR(30)     NOT NULL,
    puntaje             INTEGER         NOT NULL DEFAULT 0,
    UNIQUE(probabilidad, impacto)
);

INSERT INTO proyecta_db.matriz_riesgo (probabilidad, impacto, nivel_resultante, puntaje) VALUES
('BAJA',  'BAJO',  'BAJO', 1),
('BAJA',  'MEDIO', 'BAJO', 2),
('BAJA',  'ALTO',  'MODERADO', 3),
('MEDIA', 'BAJO',  'BAJO', 2),
('MEDIA', 'MEDIO', 'MODERADO', 4),
('MEDIA', 'ALTO',  'ALTO', 6),
('ALTA',  'BAJO',  'MODERADO', 3),
('ALTA',  'MEDIO', 'ALTO', 6),
('ALTA',  'ALTO',  'CRITICO', 9);

-- 6b. Estado Riesgo Config
CREATE TABLE IF NOT EXISTS proyecta_db.estado_riesgo_config (
    estado_riesgo_id    SERIAL          PRIMARY KEY,
    codigo              VARCHAR(30)     NOT NULL UNIQUE,
    nombre              VARCHAR(100)    NOT NULL,
    descripcion         VARCHAR(300),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO proyecta_db.estado_riesgo_config (codigo, nombre, descripcion, activo) VALUES
('IDENTIFICADO', 'Identificado', 'Riesgo identificado sin tratamiento', true),
('MITIGADO',     'Mitigado',     'Riesgo con plan de mitigacion aplicado', true),
('ACEPTADO',     'Aceptado',     'Riesgo aceptado por la direccion', true),
('CERRADO',      'Cerrado',      'Riesgo cerrado o materializado', true);

-- 7. Add FK from entregable to estado_entregable_config (nullable for migration safety)
ALTER TABLE proyecta_db.entregable ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES proyecta_db.estado_entregable_config(estado_entregable_id);

-- Backfill estado_config_id from existing estado values
UPDATE proyecta_db.entregable e SET estado_config_id = (SELECT estado_entregable_id FROM proyecta_db.estado_entregable_config WHERE codigo = e.estado);

-- 8. Add FK from proyecto to estado_proyecto_config (nullable for migration safety)
ALTER TABLE proyecta_db.proyecto ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES proyecta_db.estado_proyecto_config(estado_proyecto_id);

-- Backfill estado_config_id from existing estado values
UPDATE proyecta_db.proyecto p SET estado_config_id = (SELECT estado_proyecto_id FROM proyecta_db.estado_proyecto_config WHERE codigo = p.estado);

-- 9. Add FK from usuario to rol_config (nullable for migration safety)
ALTER TABLE proyecta_db.usuario ADD COLUMN IF NOT EXISTS rol_config_id INTEGER REFERENCES proyecta_db.rol_config(rol_id);

-- Backfill rol_config_id from existing rol values
UPDATE proyecta_db.usuario u SET rol_config_id = (SELECT rol_id FROM proyecta_db.rol_config WHERE codigo = u.rol);

-- 10. Add FK from documento to tipo_documento_config (nullable for migration safety)
ALTER TABLE proyecta_db.documento ADD COLUMN IF NOT EXISTS tipo_documento_config_id INTEGER REFERENCES proyecta_db.tipo_documento_config(tipo_documento_id);

-- Backfill tipo_documento_config_id from existing tipo_documento values
UPDATE proyecta_db.documento d SET tipo_documento_config_id = (SELECT tipo_documento_id FROM proyecta_db.tipo_documento_config WHERE codigo = d.tipo_documento::text);

-- 10b. Add FK from documento_dinamico to tipo_documento_config (nullable for migration safety)
ALTER TABLE proyecta_db.documento_dinamico ADD COLUMN IF NOT EXISTS tipo_documento_config_id INTEGER REFERENCES proyecta_db.tipo_documento_config(tipo_documento_id);

-- Backfill tipo_documento_config_id from existing tipo_documento values
UPDATE proyecta_db.documento_dinamico d SET tipo_documento_config_id = (SELECT tipo_documento_id FROM proyecta_db.tipo_documento_config WHERE codigo = UPPER(d.tipo_documento));

-- 11. Add FK from proyecto to estrategia_peti_config (nullable for migration safety)
ALTER TABLE proyecta_db.proyecto ADD COLUMN IF NOT EXISTS estrategia_peti_config_id INTEGER REFERENCES proyecta_db.estrategia_peti_config(estrategia_peti_id);

-- Backfill estrategia_peti_config_id from existing estrategia_peti values
UPDATE proyecta_db.proyecto p SET estrategia_peti_config_id = (SELECT estrategia_peti_id FROM proyecta_db.estrategia_peti_config WHERE codigo = p.estrategia_peti);

-- 12. Add FK from riesgos to estado_riesgo_config (nullable for migration safety)
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS estado_config_id INTEGER REFERENCES proyecta_db.estado_riesgo_config(estado_riesgo_id);

-- Backfill estado_config_id from existing estado values
UPDATE proyecta_db.riesgos r SET estado_config_id = (SELECT estado_riesgo_id FROM proyecta_db.estado_riesgo_config WHERE codigo = UPPER(r.estado));

-- 13. Add FK from riesgos to matriz_riesgo for probabilidad and impacto
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS probabilidad_config VARCHAR(30);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS impacto_config VARCHAR(30);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS puntaje INTEGER;

-- Backfill probabilidad and impacto from numeric values
UPDATE proyecta_db.riesgos SET probabilidad_config = CASE 
    WHEN probabilidad = 1 THEN 'BAJA'
    WHEN probabilidad = 2 THEN 'BAJA'
    WHEN probabilidad = 3 THEN 'MEDIA'
    WHEN probabilidad = 4 THEN 'ALTA'
    WHEN probabilidad = 5 THEN 'ALTA'
    ELSE 'MEDIA'
END;

UPDATE proyecta_db.riesgos SET impacto_config = CASE 
    WHEN impacto = 1 THEN 'BAJO'
    WHEN impacto = 2 THEN 'BAJO'
    WHEN impacto = 3 THEN 'MEDIO'
    WHEN impacto = 4 THEN 'ALTO'
    WHEN impacto = 5 THEN 'ALTO'
    ELSE 'MEDIO'
END;

UPDATE proyecta_db.riesgos SET puntaje = probabilidad * impacto;



-- MIGRATION: V6__audit_ponderaciones_fases.sql --

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



-- MIGRATION: V7__add_miembro_cargo_and_cleanup.sql --

-- V7: Agrega columna miembro_cargo a proyecto_equipo y garantiza consistencia

ALTER TABLE proyecta_db.proyecto_equipo ADD COLUMN IF NOT EXISTS miembro_cargo VARCHAR(100);



-- MIGRATION: V8__security_access_control.sql --

CREATE SCHEMA IF NOT EXISTS proyecta_db;

CREATE TABLE IF NOT EXISTS proyecta_db.usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(120) NOT NULL UNIQUE,
    username VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(180) NOT NULL,
    correo VARCHAR(200) NOT NULL UNIQUE,
    dependencia VARCHAR(180),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.roles (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300),
    transversal BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.permisos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(400),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proyecta_db.rol_permiso (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES proyecta_db.roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES proyecta_db.permisos(id) ON DELETE CASCADE,
    CONSTRAINT uk_rol_permiso UNIQUE (rol_id, permiso_id)
);

CREATE TABLE IF NOT EXISTS proyecta_db.usuario_proyecto (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    proyecto_id VARCHAR(30) NOT NULL,
    cargo VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_proyecto_usuario FOREIGN KEY (usuario_id) REFERENCES proyecta_db.usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_proyecto_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    CONSTRAINT uk_usuario_proyecto UNIQUE (usuario_id, proyecto_id, cargo)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON proyecta_db.usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON proyecta_db.usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_roles_codigo ON proyecta_db.roles(codigo);
CREATE INDEX IF NOT EXISTS idx_permisos_codigo ON proyecta_db.permisos(codigo);
CREATE INDEX IF NOT EXISTS idx_rol_permiso_rol ON proyecta_db.rol_permiso(rol_id);
CREATE INDEX IF NOT EXISTS idx_rol_permiso_permiso ON proyecta_db.rol_permiso(permiso_id);
CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_usuario ON proyecta_db.usuario_proyecto(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_proyecto_proyecto ON proyecta_db.usuario_proyecto(proyecto_id);

INSERT INTO proyecta_db.roles (codigo, nombre, descripcion, transversal)
VALUES
    ('admin', 'Administrador', 'Control total de la plataforma', TRUE),
    ('gestor_tic', 'Gestor TIC', 'Administracion tecnica y transversal', TRUE),
    ('director_proyecto', 'Director de Proyecto', 'Operacion sobre sus proyectos asignados', FALSE),
    ('auditor', 'Auditor', 'Consulta y revision sin edicion', FALSE),
    ('consulta', 'Consulta', 'Solo lectura', FALSE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO proyecta_db.permisos (codigo, nombre, descripcion)
VALUES
    ('DASHBOARD:VER', 'Ver dashboard', 'Permite consultar la portada y resumen principal del sistema'),
    ('PROYECTO:VER', 'Ver proyecto', 'Permite consultar el detalle de proyectos'),
    ('PROYECTO:CREAR', 'Crear proyecto', 'Permite crear proyectos nuevos'),
    ('PROYECTO:EDITAR', 'Editar proyecto', 'Permite editar proyectos existentes'),
    ('PROYECTO:CERRAR', 'Cerrar proyecto', 'Permite cerrar proyectos'),
    ('REPORTE:VER', 'Ver reportes', 'Permite acceder al modulo de reportes'),
    ('ANALITICA:VER', 'Ver analiticas', 'Permite acceder al modulo de analiticas'),
    ('CONFIGURACION:VER', 'Ver configuracion', 'Permite mostrar la pantalla de administracion y seguridad'),
    ('ENTREGABLE:APROBAR', 'Aprobar entregable', 'Permite marcar entregables como conformes'),
    ('EVIDENCIA:CARGAR', 'Cargar evidencia', 'Permite subir evidencias PDF'),
    ('DOCUMENTO:CARGAR', 'Cargar documento', 'Permite subir documentos de soporte'),
    ('CRONOGRAMA:CARGAR', 'Cargar cronograma', 'Permite subir el PDF del cronograma'),
    ('SISTEMA:CONFIGURAR', 'Configurar sistema', 'Permite administrar usuarios, roles y permisos')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
CROSS JOIN proyecta_db.permisos p
WHERE r.codigo IN ('admin', 'gestor_tic')
  AND p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'PROYECTO:CREAR', 'PROYECTO:EDITAR', 'PROYECTO:CERRAR',
                   'REPORTE:VER', 'ANALITICA:VER', 'CONFIGURACION:VER',
                   'ENTREGABLE:APROBAR', 'EVIDENCIA:CARGAR', 'DOCUMENTO:CARGAR',
                   'CRONOGRAMA:CARGAR', 'SISTEMA:CONFIGURAR')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'REPORTE:VER', 'ANALITICA:VER',
                                             'ENTREGABLE:APROBAR', 'EVIDENCIA:CARGAR', 'DOCUMENTO:CARGAR',
                                             'CRONOGRAMA:CARGAR')
WHERE r.codigo = 'director_proyecto'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'REPORTE:VER', 'ANALITICA:VER')
WHERE r.codigo = 'auditor'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, TRUE
FROM proyecta_db.roles r
JOIN proyecta_db.permisos p ON p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'REPORTE:VER', 'ANALITICA:VER')
WHERE r.codigo = 'consulta'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

COMMENT ON TABLE proyecta_db.usuarios IS 'Usuarios sincronizados desde Keycloak para control de acceso y asignaciones por proyecto';
COMMENT ON TABLE proyecta_db.roles IS 'Catalogo dinamico de roles de negocio';
COMMENT ON TABLE proyecta_db.permisos IS 'Catalogo dinamico de permisos atomicos';
COMMENT ON TABLE proyecta_db.rol_permiso IS 'Matriz dinamica de asignacion de permisos a roles';
COMMENT ON TABLE proyecta_db.usuario_proyecto IS 'Asignacion explicita de usuarios a proyectos y cargos';



-- MIGRATION: V9__add_keycloak_sub_to_usuario.sql --

ALTER TABLE proyecta_db.usuario
    ADD COLUMN IF NOT EXISTS keycloak_sub VARCHAR(120) UNIQUE;

COMMENT ON COLUMN proyecta_db.usuario.keycloak_sub IS 'Identificador estable del usuario en Keycloak (claim sub)';



-- MIGRATION: V10__add_snapshot_fields_to_acta_cierre.sql --

ALTER TABLE IF EXISTS proyecta_db.actas_cierre
    ADD COLUMN IF NOT EXISTS progreso_programado_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS progreso_ejecutado_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS diferencia_final NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS eficacia_final NUMERIC(6,4),
    ADD COLUMN IF NOT EXISTS estado_final VARCHAR(30),
    ADD COLUMN IF NOT EXISTS corte_calculo DATE,
    ADD COLUMN IF NOT EXISTS snapshot_json TEXT;



-- MIGRATION: V11__add_risk_matrix_columns.sql --

ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS categoria_riesgo VARCHAR(120);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS causa TEXT;
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS consecuencia TEXT;
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS controles_existentes TEXT;
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS tipo_control VARCHAR(80);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS valoracion_control VARCHAR(80);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS probabilidad_residual VARCHAR(20);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS impacto_residual VARCHAR(20);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS nivel_residual VARCHAR(20);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS acciones_mitigacion TEXT;
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS entidad_responsable VARCHAR(150);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS rol_responsable VARCHAR(150);
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS fecha_accion DATE;
ALTER TABLE proyecta_db.riesgos ADD COLUMN IF NOT EXISTS evidencia_indicador TEXT;


