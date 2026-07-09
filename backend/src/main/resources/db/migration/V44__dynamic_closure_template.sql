CREATE TABLE IF NOT EXISTS proyecta_db.project_closure_template (
    id BIGSERIAL PRIMARY KEY,
    codigo_proceso VARCHAR(30) NOT NULL DEFAULT 'A-GT-FR-004',
    version_num INTEGER NOT NULL DEFAULT 1,
    nombre_documento VARCHAR(200) NOT NULL DEFAULT 'Acta de Cierre del Proyecto',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    template_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(80),
    updated_by VARCHAR(80)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_closure_template_active
    ON proyecta_db.project_closure_template (activo) WHERE activo = TRUE;

INSERT INTO proyecta_db.project_closure_template
    (codigo_proceso, version_num, nombre_documento, activo, template_json, created_by, updated_by)
VALUES (
    'A-GT-FR-004', 1, 'Acta de Cierre del Proyecto', TRUE,
    '{
        "secciones": [
            {
                "id": "informacion_general",
                "titulo": "Informacion General del Proyecto",
                "orden": 1,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "codigo_proyecto", "label": "Codigo del Proyecto", "tipo_input": "texto_corto"},
                    {"id": "nombre_proyecto", "label": "Nombre del Proyecto", "tipo_input": "texto_corto"},
                    {"id": "patrocinador", "label": "Patrocinador", "tipo_input": "texto_corto"},
                    {"id": "director", "label": "Director del Proyecto", "tipo_input": "texto_corto"},
                    {"id": "fecha_inicio", "label": "Fecha de Inicio", "tipo_input": "fecha"},
                    {"id": "fecha_cierre", "label": "Fecha de Cierre", "tipo_input": "fecha"},
                    {"id": "duracion_meses", "label": "Duracion Total (meses)", "tipo_input": "texto_corto"}
                ]
            },
            {
                "id": "objetivos",
                "titulo": "Objetivos del Proyecto",
                "orden": 2,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "objetivo_general", "label": "Objetivo General", "tipo_input": "texto_largo"},
                    {"id": "objetivos_especificos", "label": "Objetivos Especificos", "tipo_input": "texto_largo"}
                ]
            },
            {
                "id": "resumen_ejecutivo",
                "titulo": "Resumen Ejecutivo",
                "orden": 3,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "resumen_ejecutivo", "label": "Resumen Ejecutivo", "tipo_input": "texto_largo"}
                ]
            },
            {
                "id": "entregables",
                "titulo": "Entregables del Proyecto",
                "orden": 4,
                "tipo_seccion": "tabla",
                "columnas": [
                    {"id": "nombre_entregable", "label": "Entregable"},
                    {"id": "fecha_entrega", "label": "Fecha de Entrega"},
                    {"id": "evidencia", "label": "Evidencia Cargada"},
                    {"id": "estado", "label": "Estado"}
                ]
            },
            {
                "id": "lecciones_aprendidas",
                "titulo": "Lecciones Aprendidas",
                "orden": 5,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "lecciones_positivas", "label": "Aspectos Positivos", "tipo_input": "texto_largo"},
                    {"id": "lecciones_mejorar", "label": "Aspectos a Mejorar", "tipo_input": "texto_largo"}
                ]
            },
            {
                "id": "recomendaciones",
                "titulo": "Recomendaciones",
                "orden": 6,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "recomendaciones", "label": "Recomendaciones para futuros proyectos", "tipo_input": "texto_largo"}
                ]
            },
            {
                "id": "transferencia_conocimiento",
                "titulo": "Transferencia de Conocimiento",
                "orden": 7,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "transferencia_actividad", "label": "Actividad de Transferencia", "tipo_input": "texto_largo"},
                    {"id": "transferencia_fecha", "label": "Fecha de Transferencia", "tipo_input": "fecha"},
                    {"id": "transferencia_ubicacion", "label": "Ubicacion de la Evidencia", "tipo_input": "texto_corto"}
                ]
            },
            {
                "id": "consolidado_cierre",
                "titulo": "Consolidado de Cierre",
                "orden": 8,
                "tipo_seccion": "formulario",
                "campos": [
                    {"id": "avance_final", "label": "Avance Final (%)", "tipo_input": "texto_corto"},
                    {"id": "progreso_programado", "label": "Progreso Programado (%)", "tipo_input": "texto_corto"},
                    {"id": "progreso_ejecutado", "label": "Progreso Ejecutado (%)", "tipo_input": "texto_corto"},
                    {"id": "diferencia", "label": "Diferencia (%)", "tipo_input": "texto_corto"},
                    {"id": "eficacia", "label": "Eficacia", "tipo_input": "texto_corto"}
                ]
            }
        ]
    }'::jsonb,
    'system', 'system'
);

CREATE TABLE IF NOT EXISTS proyecta_db.project_closures (
    id BIGSERIAL PRIMARY KEY,
    proyecto_id VARCHAR(30) NOT NULL,
    template_id BIGINT NOT NULL REFERENCES proyecta_db.project_closure_template(id),
    template_snapshot JSONB NOT NULL,
    form_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(80),
    UNIQUE(proyecto_id)
);
