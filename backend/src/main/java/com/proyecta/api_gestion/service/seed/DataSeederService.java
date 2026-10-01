package com.proyecta.api_gestion.service.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquestador de sembrado de datos.
 * 
 * Responsabilidad: Coordinar la carga de todas las entidades semilla en orden correcto.
 * Patrón: Facade + Dependency Injection (DIP)
 */
@Service
public class DataSeederService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSeederService.class);

    private final UsuarioSeeder usuarioSeeder;
    private final PatrocinadorSeeder patrocinadorSeeder;
    private final SystemParameterSeeder systemParameterSeeder;
    private final ReporteConfigSeeder reporteConfigSeeder;
    private final SecurityCatalogSeeder securityCatalogSeeder;

    public DataSeederService(
            UsuarioSeeder usuarioSeeder,
            PatrocinadorSeeder patrocinadorSeeder,
            SystemParameterSeeder systemParameterSeeder,
            ReporteConfigSeeder reporteConfigSeeder,
            SecurityCatalogSeeder securityCatalogSeeder) {
        this.usuarioSeeder = usuarioSeeder;
        this.patrocinadorSeeder = patrocinadorSeeder;
        this.systemParameterSeeder = systemParameterSeeder;
        this.reporteConfigSeeder = reporteConfigSeeder;
        this.securityCatalogSeeder = securityCatalogSeeder;
    }

    /**
     * Ejecuta la carga completa de datos semilla.
     * El orden es importante: dependencias primero, luego entidades relacionadas.
     */
    public void seedAllData() {
        // Orden: Entidades independientes primero, luego las dependientes.
        // Cada seeder se ejecuta de forma independiente: un fallo no aborta los demas.
        ejecutarPaso("usuarios", usuarioSeeder::seedUsuarios);
        ejecutarPaso("catalogo seguridad", securityCatalogSeeder::seedSecurityCatalog);
        ejecutarPaso("patrocinadores", patrocinadorSeeder::seedPatrocinadores);
        ejecutarPaso("parametros sistema", systemParameterSeeder::seedSystemParameters);
        ejecutarPaso("configs reportes", reporteConfigSeeder::seedReporteConfigs);
    }

    private void ejecutarPaso(String nombre, Runnable paso) {
        try {
            paso.run();
        } catch (Exception ex) {
            logger.warn("⚠ Seeder '{}' falto (continuando con los demas): {}", nombre, ex.getMessage());
        }
    }
}
