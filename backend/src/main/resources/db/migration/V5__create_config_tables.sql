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
