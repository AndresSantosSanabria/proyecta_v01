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
