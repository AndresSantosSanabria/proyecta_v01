package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea la tabla advance_report_version y las columnas de auditoria de
 * advance_report_uploads en BDs existentes. Flyway ignora migraciones pendientes
 * en este proyecto, por lo que el esquema se asegura aqui de forma idempotente.
 * En BDs fresh la tabla base ya viene de V1; CREATE TABLE IF NOT EXISTS es seguro.
 */
@Component
public class AdvanceReportSchemaEnsurer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportSchemaEnsurer.class);

    private final JdbcTemplate jdbcTemplate;

    public AdvanceReportSchemaEnsurer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            addColumnIfMissing("advance_report_uploads", "verified_by", "VARCHAR(120)");
            addColumnIfMissing("advance_report_uploads", "verified_at", "TIMESTAMP");
            addColumnIfMissing("advance_report_uploads", "returned_by", "VARCHAR(120)");
            addColumnIfMissing("advance_report_uploads", "returned_at", "TIMESTAMP");
            addColumnIfMissing("advance_report_uploads", "subido_rol", "VARCHAR(60)");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS advance_report_version (
                    id              BIGSERIAL    PRIMARY KEY,
                    upload_id       BIGINT       NOT NULL REFERENCES advance_report_uploads(id),
                    project_id      VARCHAR(40)  NOT NULL,
                    periodo         VARCHAR(20)  NOT NULL,
                    numero_version  INT          NOT NULL,
                    file_name       VARCHAR(255) NOT NULL,
                    file_path       VARCHAR(500) NOT NULL,
                    file_size       BIGINT,
                    mime_type       VARCHAR(120),
                    estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTUAL',
                    observacion     VARCHAR(1000),
                    subido_por      VARCHAR(120),
                    subido_rol      VARCHAR(60),
                    subido_en       TIMESTAMP    DEFAULT NOW(),
                    UNIQUE (upload_id, numero_version)
                )
                """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_adv_version_upload ON advance_report_version(upload_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_adv_version_project_periodo ON advance_report_version(project_id, periodo)");

            backfillVersions();
            log.info("✓ Tabla advance_report_version y auditoria de advance_report_uploads aseguradas");
        } catch (Exception e) {
            log.warn("⚠ No se pudo asegurar esquema de informes de avance: {}", e.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String type) {
        try {
            Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'proyecta_db' AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
            if (exists == null || exists == 0) {
                exists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_name = ? AND column_name = ?
                    """, Integer.class, table, column);
            }
            if (exists != null && exists > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("✓ Columno agregada: {}.{}", table, column);
        } catch (Exception e) {
            log.warn("⚠ No se pudo verificar/agregar columna {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void backfillVersions() {
        Integer pending = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM advance_report_uploads u
            WHERE NOT EXISTS (SELECT 1 FROM advance_report_version v WHERE v.upload_id = u.id)
            """, Integer.class);
        if (pending == null || pending == 0) {
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO advance_report_version
                (upload_id, project_id, periodo, numero_version, file_name, file_path, file_size,
                 estado, subido_por, subido_en)
            SELECT u.id, u.project_id, u.periodo, 1, u.file_name, u.file_path, u.file_size,
                   'ACTUAL', u.uploaded_by, u.uploaded_at
            FROM advance_report_uploads u
            WHERE NOT EXISTS (SELECT 1 FROM advance_report_version v WHERE v.upload_id = u.id)
            """);
        log.info("✓ {} informe(s) existente(s) migrado(s) a version 1", pending);
    }
}
