CREATE TABLE IF NOT EXISTS proyecta_db.lista_parametrica_config (
    id BIGSERIAL PRIMARY KEY,
    lista_clave VARCHAR(60) NOT NULL,
    item_codigo VARCHAR(80) NOT NULL,
    item_nombre VARCHAR(200) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_lista_item UNIQUE (lista_clave, item_codigo)
);

CREATE INDEX IF NOT EXISTS idx_lista_parametrica_clave ON proyecta_db.lista_parametrica_config (lista_clave);

INSERT INTO proyecta_db.lista_parametrica_config (lista_clave, item_codigo, item_nombre, orden, activo)
VALUES
  ('DEPENDENCIA', 'INFRAESTRUCTURA', 'Infraestructura', 1, true),
  ('DEPENDENCIA', 'ATENCION_CIUDADANO', 'Atencion al Ciudadano', 2, true),
  ('DEPENDENCIA', 'FINANZAS', 'Finanzas', 3, true),
  ('DEPENDENCIA', 'PRENSA_COMUNICACIONES', 'Prensa y Comunicaciones', 4, true),
  ('DEPENDENCIA', 'SEGURIDAD_INFORMACION', 'Seguridad de la Informacion', 5, true),
  ('DEPENDENCIA', 'INNOVACION_TECNOLOGIA', 'Innovacion y Tecnologia', 6, true),
  ('DEPENDENCIA', 'CALIDAD_SOFTWARE', 'Calidad de Software', 7, true),
  ('DEPENDENCIA', 'PLANEACION', 'Planeacion', 8, true),
  ('DEPENDENCIA', 'JURIDICA', 'Juridica', 9, true),
  ('DEPENDENCIA', 'TALENTO_HUMANO', 'Talento Humano', 10, true)
ON CONFLICT (lista_clave, item_codigo) DO NOTHING;
