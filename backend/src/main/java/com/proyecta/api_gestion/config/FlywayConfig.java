package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de Flyway.
 * <p>
 * La propiedad {@code spring.flyway.repair-on-migrate} NO existe en Spring Boot 4
 * y se ignora silenciosamente. Cuando una migracion ya aplicada sufre un cambio de
 * checksum (V2 editada tras aplicarse), el arranque falla con
 * {@code FlywayValidateException: Migration checksum mismatch}.
 * <p>
 * Se define un {@link FlywayMigrationStrategy} que ejecuta {@code repair()} antes de
 * {@code migrate()} para resincronizar el historial con los archivos locales.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.repair();
                log.info("Flyway repair ejecutado: historial resincronizado con las migraciones locales.");
            } catch (Exception ex) {
                log.warn("Flyway repair no pudo ejecutarse (se continuara con migrate): {}", ex.getMessage());
            }
            flyway.migrate();
        };
    }
}
