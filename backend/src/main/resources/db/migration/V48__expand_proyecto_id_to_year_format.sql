-- Expande proyecto_id de VARCHAR(30) a VARCHAR(40) en la tabla proyecto
-- para soportar la nomenclatura IS-PROY-CUN-YYYY-NNN (20 caracteres).
-- Las demas tablas que referencian proyecto_id con VARCHAR(30) no necesitan cambio
-- porque el valor encaja y PostgreSQL no exige igualdad de tamaño en FK.

ALTER TABLE proyecta_db.proyecto
    ALTER COLUMN proyecto_id TYPE VARCHAR(40);

COMMENT ON TABLE proyecta_db.proyecto IS 'Proyectos de gestion tecnologica. PK manual con formato IS-PROY-CUN-YYYY-NNN.';
