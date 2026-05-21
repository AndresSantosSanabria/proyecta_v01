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
