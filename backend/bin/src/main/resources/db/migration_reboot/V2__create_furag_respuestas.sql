CREATE TABLE IF NOT EXISTS proyecta_db.respuestas_furag (
    respuesta_furag_id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    codigo_pregunta VARCHAR(80) NOT NULL,
    pregunta TEXT NOT NULL,
    respuesta VARCHAR(5),
    obligatoria BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_respuestas_furag_proyecto
        FOREIGN KEY (proyecto_id) REFERENCES proyecta_db.proyecto(proyecto_id) ON DELETE CASCADE,
    CONSTRAINT uk_respuestas_furag UNIQUE (proyecto_id, codigo_pregunta)
);

CREATE INDEX IF NOT EXISTS idx_respuestas_furag_proyecto
    ON proyecta_db.respuestas_furag(proyecto_id);

COMMENT ON TABLE proyecta_db.respuestas_furag IS 'Respuestas FURAG normalizadas por proyecto para control de bloqueo de reportes.';
