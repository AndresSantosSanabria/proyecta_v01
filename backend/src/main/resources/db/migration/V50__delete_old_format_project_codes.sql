DELETE FROM proyecta_db.actas_cierre
WHERE proyecto_id IN (
    SELECT proyecto_id
    FROM proyecta_db.proyecto
    WHERE proyecto_id ~ '^IS-PROY-CUN-[0-9]+$'
);

DELETE FROM proyecta_db.proyecto
WHERE proyecto_id ~ '^IS-PROY-CUN-[0-9]+$';
