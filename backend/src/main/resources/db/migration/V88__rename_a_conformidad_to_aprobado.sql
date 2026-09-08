-- V88: Rename A_CONFORMIDAD to APROBADO in entregable state system

-- 1. Drop old CHECK constraint
ALTER TABLE entregable
DROP CONSTRAINT IF EXISTS entregable_estado_check;

-- 2. Update seed data in estado_entregable_config
UPDATE estado_entregable_config
SET codigo = 'APROBADO',
    nombre = 'Aprobado',
    descripcion = 'Entregable aprobado formalmente por el gestor'
WHERE codigo = 'A_CONFORMIDAD';

-- 3. Update existing deliverable records BEFORE adding constraint
UPDATE entregable
SET estado = 'APROBADO'
WHERE estado = 'A_CONFORMIDAD';

-- 4. Now add new CHECK constraint (all rows already compliant)
ALTER TABLE entregable
ADD CONSTRAINT entregable_estado_check
CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'RECHAZADO', 'APROBADO', 'COMPLETADO', 'ATRASADO'));
