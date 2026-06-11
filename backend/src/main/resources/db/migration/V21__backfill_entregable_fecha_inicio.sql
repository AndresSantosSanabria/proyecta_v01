UPDATE entregable e
SET fecha_inicio = COALESCE(p.fecha_inicio, e.fecha_limite)
FROM hito h
JOIN fase f ON h.fase_id = f.fase_id
JOIN proyecto p ON f.proyecto_id = p.proyecto_id
WHERE e.hito_id = h.hito_id
  AND e.fecha_inicio IS NULL
  AND COALESCE(p.fecha_inicio, e.fecha_limite) IS NOT NULL;
