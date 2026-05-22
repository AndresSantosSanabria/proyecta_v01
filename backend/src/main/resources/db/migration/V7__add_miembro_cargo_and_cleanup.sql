-- V7: Agrega columna miembro_cargo a proyecto_equipo y garantiza consistencia

ALTER TABLE proyecto_equipo ADD COLUMN IF NOT EXISTS miembro_cargo VARCHAR(100);
