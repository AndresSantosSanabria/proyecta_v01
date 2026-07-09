CREATE TABLE IF NOT EXISTS proyecta_db.project_closure_question (
    id BIGSERIAL PRIMARY KEY,
    texto VARCHAR(500) NOT NULL,
    tipo_respuesta VARCHAR(30) NOT NULL DEFAULT 'texto_libre',
    opciones JSONB,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(80),
    updated_by VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS proyecta_db.project_closure_answer (
    id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    question_id BIGINT NOT NULL REFERENCES proyecta_db.project_closure_question(id),
    respuesta TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(proyecto_id, question_id)
);

INSERT INTO proyecta_db.project_closure_question (texto, tipo_respuesta, activo, orden, created_by, updated_by) VALUES
('Resumen Ejecutivo del Proyecto', 'texto_libre', TRUE, 1, 'system', 'system'),
('Lecciones aprendidas: aspectos positivos', 'texto_libre', TRUE, 2, 'system', 'system'),
('Lecciones aprendidas: aspectos a mejorar', 'texto_libre', TRUE, 3, 'system', 'system'),
('Recomendaciones para futuros proyectos', 'texto_libre', TRUE, 4, 'system', 'system'),
('Actividad de transferencia de conocimiento ejecutada', 'texto_libre', TRUE, 5, 'system', 'system'),
('Ubicacion de la evidencia de transferencia', 'texto_libre', TRUE, 6, 'system', 'system');
