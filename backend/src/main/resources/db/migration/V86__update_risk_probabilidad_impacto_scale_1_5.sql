-- Step 1: Drop OLD constraints first so UPDATE statements don't fail
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_probabilidad_check;
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_impacto_check;
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_probabilidad_residual_check;
ALTER TABLE proyecta_db.riesgos DROP CONSTRAINT IF EXISTS riesgos_impacto_residual_check;

-- Step 2: Migrate existing data from old scale to new 1-5 scale
UPDATE proyecta_db.riesgos SET probabilidad = 'DOS' WHERE probabilidad = 'BAJA';
UPDATE proyecta_db.riesgos SET probabilidad = 'TRES' WHERE probabilidad = 'MEDIA';
UPDATE proyecta_db.riesgos SET probabilidad = 'CUATRO' WHERE probabilidad = 'ALTA';
UPDATE proyecta_db.riesgos SET impacto = 'DOS' WHERE impacto = 'BAJO';
UPDATE proyecta_db.riesgos SET impacto = 'TRES' WHERE impacto = 'MEDIO';
UPDATE proyecta_db.riesgos SET impacto = 'CUATRO' WHERE impacto = 'ALTO';

-- Also migrate residual risk columns
UPDATE proyecta_db.riesgos SET probabilidad_residual = 'DOS' WHERE probabilidad_residual = 'BAJA';
UPDATE proyecta_db.riesgos SET probabilidad_residual = 'TRES' WHERE probabilidad_residual = 'MEDIA';
UPDATE proyecta_db.riesgos SET probabilidad_residual = 'CUATRO' WHERE probabilidad_residual = 'ALTA';
UPDATE proyecta_db.riesgos SET impacto_residual = 'DOS' WHERE impacto_residual = 'BAJO';
UPDATE proyecta_db.riesgos SET impacto_residual = 'TRES' WHERE impacto_residual = 'MEDIO';
UPDATE proyecta_db.riesgos SET impacto_residual = 'CUATRO' WHERE impacto_residual = 'ALTO';

-- Step 3: Add NEW constraints with 1-5 scale values
ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_probabilidad_check
    CHECK (probabilidad IN ('UNO', 'DOS', 'TRES', 'CUATRO', 'CINCO'));

ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_impacto_check
    CHECK (impacto IN ('UNO', 'DOS', 'TRES', 'CUATRO', 'CINCO'));

ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_probabilidad_residual_check
    CHECK (probabilidad_residual IN ('UNO', 'DOS', 'TRES', 'CUATRO', 'CINCO'));

ALTER TABLE proyecta_db.riesgos ADD CONSTRAINT riesgos_impacto_residual_check
    CHECK (impacto_residual IN ('UNO', 'DOS', 'TRES', 'CUATRO', 'CINCO'));
