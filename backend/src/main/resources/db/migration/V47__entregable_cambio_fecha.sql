CREATE TABLE entregable_cambio_fecha (
    id              BIGSERIAL       PRIMARY KEY,
    entregable_id   INTEGER         NOT NULL REFERENCES entregable(entregable_id),
    fecha_anterior  DATE            NOT NULL,
    fecha_nueva     DATE            NOT NULL,
    justificacion   TEXT            NOT NULL,
    archivo_pdf     VARCHAR(300)    NOT NULL,
    nombre_original VARCHAR(300),
    usuario         VARCHAR(100)    NOT NULL,
    usuario_rol     VARCHAR(50),
    creado_en       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cambio_fecha_entregable ON entregable_cambio_fecha(entregable_id);
