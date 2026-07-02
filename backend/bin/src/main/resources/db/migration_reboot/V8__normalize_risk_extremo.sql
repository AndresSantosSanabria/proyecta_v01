-- V8: Normalize risk matrix levels to match the institutional Excel template.
-- The workbook uses "Extremo" as the highest level for the 8-10 range.

UPDATE proyecta_db.matriz_riesgo
SET nivel_riesgo = 'EXTREMO',
    color = '#dc2626',
    puntaje = 9
WHERE UPPER(nivel_riesgo) = 'CRITICO'
   OR (UPPER(probabilidad) = 'ALTA' AND UPPER(impacto) = 'ALTO');

UPDATE proyecta_db.riesgos
SET nivel = 'EXTREMO'
WHERE UPPER(nivel) = 'CRITICO';

UPDATE proyecta_db.riesgos
SET nivel_residual = 'EXTREMO'
WHERE UPPER(nivel_residual) = 'CRITICO';

UPDATE proyecta_db.riesgos
SET nivel = CASE
        WHEN UPPER(probabilidad) = 'ALTA' AND UPPER(impacto) = 'ALTO' THEN 'EXTREMO'
        WHEN UPPER(probabilidad) = 'MEDIA' AND UPPER(impacto) = 'ALTO' THEN 'ALTO'
        WHEN UPPER(probabilidad) = 'ALTA' AND UPPER(impacto) = 'MEDIO' THEN 'ALTO'
        WHEN UPPER(probabilidad) = 'ALTA' AND UPPER(impacto) = 'BAJO' THEN 'MODERADO'
        WHEN UPPER(probabilidad) = 'MEDIA' AND UPPER(impacto) = 'MEDIO' THEN 'MODERADO'
        WHEN UPPER(probabilidad) = 'BAJA' AND UPPER(impacto) = 'ALTO' THEN 'MODERADO'
        WHEN UPPER(probabilidad) = 'MEDIA' AND UPPER(impacto) = 'BAJO' THEN 'BAJO'
        WHEN UPPER(probabilidad) = 'BAJA' AND UPPER(impacto) = 'MEDIO' THEN 'BAJO'
        ELSE 'BAJO'
    END
WHERE nivel IS NULL;

UPDATE proyecta_db.riesgos
SET nivel_residual = CASE
        WHEN UPPER(probabilidad_residual) = 'ALTA' AND UPPER(impacto_residual) = 'ALTO' THEN 'EXTREMO'
        WHEN UPPER(probabilidad_residual) = 'MEDIA' AND UPPER(impacto_residual) = 'ALTO' THEN 'ALTO'
        WHEN UPPER(probabilidad_residual) = 'ALTA' AND UPPER(impacto_residual) = 'MEDIO' THEN 'ALTO'
        WHEN UPPER(probabilidad_residual) = 'ALTA' AND UPPER(impacto_residual) = 'BAJO' THEN 'MODERADO'
        WHEN UPPER(probabilidad_residual) = 'MEDIA' AND UPPER(impacto_residual) = 'MEDIO' THEN 'MODERADO'
        WHEN UPPER(probabilidad_residual) = 'BAJA' AND UPPER(impacto_residual) = 'ALTO' THEN 'MODERADO'
        WHEN UPPER(probabilidad_residual) = 'MEDIA' AND UPPER(impacto_residual) = 'BAJO' THEN 'BAJO'
        WHEN UPPER(probabilidad_residual) = 'BAJA' AND UPPER(impacto_residual) = 'MEDIO' THEN 'BAJO'
        ELSE nivel_residual
    END
WHERE nivel_residual IS NULL;
