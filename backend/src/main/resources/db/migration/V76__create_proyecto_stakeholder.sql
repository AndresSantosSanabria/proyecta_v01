-- V76: Tabla de stakeholders (Grupo de Interes) del proyecto
-- Almacena los grupos de interes identificados para cada proyecto
-- Campos: Rol, Descripcion, Interes/Expectativas, Impacto en el Proyecto

CREATE TABLE IF NOT EXISTS proyecto_stakeholder (
    proyecto_id          VARCHAR(30)  NOT NULL REFERENCES proyecto(proyecto_id) ON DELETE CASCADE,
    stakeholder_rol      VARCHAR(150),
    stakeholder_descripcion TEXT,
    stakeholder_interes  TEXT,
    stakeholder_impacto  TEXT
);
