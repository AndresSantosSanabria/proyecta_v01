-- Aumenta la precision de presupuesto_estimado para soportar valores mayores.
-- NUMERIC(18,2) limita a 10^16. Se cambia a NUMERIC(24,2) (hasta 10^22).

ALTER TABLE proyecta_db.proyecto
    ALTER COLUMN presupuesto_estimado TYPE NUMERIC(24, 2);
