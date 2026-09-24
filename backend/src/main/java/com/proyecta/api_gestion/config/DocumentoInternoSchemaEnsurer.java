package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea la tabla documento_interno en BDs existentes donde Flyway ignora
 * migraciones pendientes (spring.flyway.ignore-migration-patterns=*:pending).
 * En BDs fresh la tabla ya viene de V1; CREATE TABLE IF NOT EXISTS es idempotente.
 * Tambien corrige tipos de columna si fueron creados como bytea en lugar de VARCHAR.
 */
@Component
public class DocumentoInternoSchemaEnsurer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentoInternoSchemaEnsurer.class);

    private final JdbcTemplate jdbcTemplate;

    public DocumentoInternoSchemaEnsurer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS documento_interno (
                    id                       BIGSERIAL    PRIMARY KEY,
                    codigo                   VARCHAR(30)  NOT NULL UNIQUE,
                    nombre                   VARCHAR(255) NOT NULL,
                    descripcion              VARCHAR(1000),
                    fecha_creacion           DATE         NOT NULL DEFAULT CURRENT_DATE,
                    nombre_original          VARCHAR(255) NOT NULL,
                    nombre_almacenado        VARCHAR(255) NOT NULL UNIQUE,
                    ruta_almacenamiento      VARCHAR(500) NOT NULL,
                    mime_type                VARCHAR(100) NOT NULL,
                    tamano_bytes             BIGINT       NOT NULL,
                    creado_por               VARCHAR(200) NOT NULL,
                    creado_en                TIMESTAMP    NOT NULL DEFAULT NOW()
                )
                """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_documento_interno_fecha ON documento_interno(fecha_creacion)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_documento_interno_nombre ON documento_interno(nombre)");
            fixColumnTypes();
            log.info("✓ Tabla documento_interno asegurada");
        } catch (Exception e) {
            log.warn("⚠ No se pudo asegurar tabla documento_interno: {}", e.getMessage());
        }
    }

    private void fixColumnTypes() {
        String[][] columns = {
            {"nombre", "VARCHAR(255)"},
            {"descripcion", "VARCHAR(1000)"},
            {"codigo", "VARCHAR(30)"},
            {"nombre_original", "VARCHAR(255)"},
            {"nombre_almacenado", "VARCHAR(255)"},
            {"ruta_almacenamiento", "VARCHAR(500)"},
            {"mime_type", "VARCHAR(100)"},
            {"creado_por", "VARCHAR(200)"}
        };

        for (String[] col : columns) {
            String columnName = col[0];
            String targetType = col[1];
            try {
                String dataType = jdbcTemplate.queryForObject("""
                    SELECT data_type FROM information_schema.columns
                    WHERE table_name = 'documento_interno' AND column_name = ?
                    """, String.class, columnName);

                if (dataType != null && dataType.equalsIgnoreCase("bytea")) {
                    jdbcTemplate.execute(
                        "ALTER TABLE documento_interno ALTER COLUMN " + columnName +
                        " TYPE " + targetType + " USING " + columnName + "::text");
                    log.info("✓ Corregido documento_interno.{}: bytea -> {}", columnName, targetType);
                }
            } catch (Exception e) {
                log.warn("⚠ No se pudo verificar/corregir columna {}: {}", columnName, e.getMessage());
            }
        }
    }
}
