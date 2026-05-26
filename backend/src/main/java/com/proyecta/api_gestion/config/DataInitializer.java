package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.service.seed.DataSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Orquestador de inicialización de datos.
 * 
 * Responsabilidad: Coordinar la carga de datos semilla al arrancar la aplicación.
 * Desacoplamiento: Delega la lógica de negocio en DataSeederService (SRP).
 */
@Configuration
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedDatabase(DataSeederService dataSeederService) {
        return args -> {
            try {
                logger.info("▶ Iniciando carga de datos semilla...");
                dataSeederService.seedAllData();
                logger.info("✓ Datos semilla cargados exitosamente");
            } catch (Exception e) {
                logger.warn("⚠ Error al cargar datos semilla (continuando sin datos): " + e.getMessage());
                // No lanzar excepción para permitir que la app continúe sin BD
            }
        };
    }
}
