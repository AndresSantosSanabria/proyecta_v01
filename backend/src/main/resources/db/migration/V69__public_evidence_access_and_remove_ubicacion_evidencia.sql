-- 1. Tabla de tokens de acceso publico a evidencias
CREATE TABLE IF NOT EXISTS proyecta_db.public_evidence_access (
    id              BIGSERIAL       PRIMARY KEY,
    entregable_id   INTEGER         NOT NULL REFERENCES proyecta_db.entregable(entregable_id) ON DELETE CASCADE,
    token           VARCHAR(64)     NOT NULL UNIQUE,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_public_evidence_token ON proyecta_db.public_evidence_access(token);
CREATE INDEX IF NOT EXISTS idx_public_evidence_entregable ON proyecta_db.public_evidence_access(entregable_id);

COMMENT ON TABLE proyecta_db.public_evidence_access IS 'Tokens de acceso publico para ver evidencias de entregables sin autenticacion';

-- 2. Desactivar pregunta de "Ubicacion de la Evidencia" (id=6)
UPDATE proyecta_db.project_closure_question
SET activo = FALSE
WHERE id = 6;

-- 3. Desactivar plantilla activa actual
UPDATE proyecta_db.project_closure_template
SET activo = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE activo = TRUE;

-- 3. Insertar nueva plantilla SIN el campo "Ubicacion de la Evidencia"
INSERT INTO proyecta_db.project_closure_template
    (codigo_proceso, version_num, nombre_documento, activo, template_json, created_by, updated_by)
VALUES (
    'A-GT-FR-004',
    3,
    'Acta de Cierre del Proyecto',
    TRUE,
    '{
      "secciones": [
        {
          "id": "datos_resueltos_proyecto",
          "titulo": "Datos Generales del Proyecto",
          "orden": 1,
          "tipo_seccion": "formulario",
          "campos": [
            {"id": "codigo_proyecto", "label": "Código del Proyecto", "tipo_input": "texto_corto", "source": "proyecto.id", "activo": true, "orden": 1, "readonly": true},
            {"id": "nombre_proyecto", "label": "Nombre del Proyecto", "tipo_input": "texto_largo", "source": "proyecto.nombre", "activo": true, "orden": 2, "readonly": true},
            {"id": "patrocinador", "label": "Patrocinador", "tipo_input": "texto_largo", "source": "proyecto.patrocinador", "activo": true, "orden": 3, "readonly": true},
            {"id": "director", "label": "Director del Proyecto", "tipo_input": "texto_corto", "source": "proyecto.director", "activo": true, "orden": 4, "readonly": true},
            {"id": "fecha_inicio", "label": "Fecha de Inicio", "tipo_input": "fecha", "source": "proyecto.fechaInicio", "activo": true, "orden": 5, "readonly": true},
            {"id": "fecha_cierre", "label": "Fecha de Cierre", "tipo_input": "fecha", "source": "sistema.fechaActual", "activo": true, "orden": 6, "readonly": true},
            {"id": "duracion_meses", "label": "Duración Total (meses)", "tipo_input": "texto_corto", "source": "sistema.duracion", "activo": true, "orden": 7, "readonly": true}
          ]
        },
        {
          "id": "contenido_cierre",
          "titulo": "Contenido del Cierre",
          "orden": 2,
          "tipo_seccion": "formulario",
          "campos": [
            {"id": "resumen_ejecutivo", "label": "Resumen Ejecutivo", "tipo_input": "texto_largo", "questionId": 1, "activo": true, "orden": 1, "requerido": true},
            {"id": "lecciones_positivas", "label": "Aspectos Positivos", "tipo_input": "texto_largo", "questionId": 2, "activo": true, "orden": 2, "requerido": true},
            {"id": "lecciones_mejorar", "label": "Aspectos a Mejorar", "tipo_input": "texto_largo", "questionId": 3, "activo": true, "orden": 3, "requerido": true},
            {"id": "recomendaciones", "label": "Recomendaciones para futuros proyectos", "tipo_input": "texto_largo", "questionId": 4, "activo": true, "orden": 4, "requerido": true},
            {"id": "transferencia_actividad", "label": "Actividad de Transferencia de Conocimiento", "tipo_input": "texto_largo", "questionId": 5, "activo": true, "orden": 5, "requerido": true}
          ]
        },
        {
          "id": "entregables",
          "titulo": "Entregables del Proyecto",
          "orden": 3,
          "tipo_seccion": "tabla",
          "columnas": [
            {"id": "nombre_entregable", "label": "Entregable", "activo": true, "orden": 1},
            {"id": "fecha_entrega", "label": "Fecha de Entrega", "activo": true, "orden": 2},
            {"id": "evidencia", "label": "Evidencia", "activo": true, "orden": 3},
            {"id": "estado", "label": "Estado", "activo": true, "orden": 4}
          ]
        },
        {
          "id": "consolidado",
          "titulo": "Consolidado Final",
          "orden": 4,
          "tipo_seccion": "formulario",
          "campos": [
            {"id": "avance_total", "label": "Avance Final (%)", "tipo_input": "texto_corto", "source": "proyecto.avanceTotal", "activo": true, "orden": 1, "readonly": true},
            {"id": "progreso_programado", "label": "Progreso Programado (%)", "tipo_input": "texto_corto", "source": "sistema.progresoProgramado", "activo": true, "orden": 2, "readonly": true},
            {"id": "progreso_ejecutado", "label": "Progreso Ejecutado (%)", "tipo_input": "texto_corto", "source": "sistema.progresoEjecutado", "activo": true, "orden": 3, "readonly": true},
            {"id": "diferencia", "label": "Diferencia (%)", "tipo_input": "texto_corto", "source": "sistema.diferencia", "activo": true, "orden": 4, "readonly": true},
            {"id": "eficacia", "label": "Eficacia", "tipo_input": "texto_corto", "source": "sistema.eficacia", "activo": true, "orden": 5, "readonly": true},
            {"id": "estado", "label": "Estado Final", "tipo_input": "texto_corto", "source": "proyecto.estado", "activo": true, "orden": 6, "readonly": true}
          ]
        }
      ]
    }'::jsonb,
    'system',
    'system'
);
