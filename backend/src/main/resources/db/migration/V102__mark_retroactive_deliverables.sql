ALTER TABLE entregable
    ADD COLUMN IF NOT EXISTS retroactivo BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE entregable e
SET retroactivo = TRUE
FROM hito h
JOIN fase f ON f.fase_id = h.fase_id
JOIN proyecto p ON p.proyecto_id = f.proyecto_id
WHERE e.hito_id = h.hito_id
  AND p.completado_por_director_at IS NOT NULL
  AND e.fecha_creacion IS NOT NULL
  AND e.fecha_limite IS NOT NULL
  AND e.fecha_limite < p.completado_por_director_at::date
  AND e.fecha_creacion::date = p.completado_por_director_at::date;
