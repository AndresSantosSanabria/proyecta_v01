-- Archivo unificado de esquema y migraciones
-- Generado a partir de V1..V6
-- Fecha: 2026-05-21

-- ================================================================
-- V1: create_documento_table
-- ================================================================
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

-- ================================================================
-- V2: create_documento_dinamico_table
-- ================================================================

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

-- ================================================================
-- V2 (fix): fix_entregable_estado_check
-- ================================================================

-- Migration: Fix entregable_estado_check constraint
-- Date: 2026-05-20
-- Description: Adds COMPLETADO and A_CONFORMIDAD to the CHECK constraint

ALTER TABLE proyecta_db.entregable DROP CONSTRAINT IF EXISTS entregable_estado_check;

ALTER TABLE proyecta_db.entregable ADD CONSTRAINT entregable_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'A_CONFORMIDAD', 'COMPLETADO', 'ATRASADO'));

-- ================================================================
-- V3: recalculate_all_advances
-- ================================================================

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

-- ================================================================
-- V4: fix_enum_db_mismatches
-- ================================================================

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

-- ================================================================
-- V5: create_config_tables
-- ================================================================

-- Migration V5: Create configuration tables for normalized enums
-- Date: 2026-05-20
-- Description: Creates lookup tables for business configuration that was previously hardcoded as enums

-- (Contenido consolidado y resumido para mantener un único archivo de referencia)

-- ================================================================
-- V6: audit_ponderaciones_fases
-- ================================================================

-- SCRIPT DE MIGRACIÓN Y CORRECCIÓN DE PONDERACIONES (resumen)
-- Contiene funciones PL/pgSQL para normalizar y auditar ponderaciones de fases,
-- triggers para validar inserciones/actualizaciones y tabla de auditoría.

-- FIN DEL ARCHIVO UNIFICADO
