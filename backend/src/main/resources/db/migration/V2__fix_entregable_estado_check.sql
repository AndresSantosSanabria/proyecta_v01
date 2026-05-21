-- Migration: Fix entregable_estado_check constraint
-- Date: 2026-05-20
-- Description: Adds COMPLETADO and A_CONFORMIDAD to the CHECK constraint

ALTER TABLE proyecta_db.entregable DROP CONSTRAINT IF EXISTS entregable_estado_check;

ALTER TABLE proyecta_db.entregable ADD CONSTRAINT entregable_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'A_CONFORMIDAD', 'COMPLETADO', 'ATRASADO'));
