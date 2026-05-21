package com.proyecta.api_gestion.service.seed;

import org.springframework.stereotype.Service;

/**
 * Orquestador de sembrado de datos.
 * 
 * Responsabilidad: Coordinar la carga de todas las entidades semilla en orden correcto.
 * Patrón: Facade + Dependency Injection (DIP)
 */
@Service
public class DataSeederService {
    
    private final UsuarioSeeder usuarioSeeder;
    private final PatrocinadorSeeder patrocinadorSeeder;
    private final SystemParameterSeeder systemParameterSeeder;
    private final ReporteConfigSeeder reporteConfigSeeder;
    private final ProyectoSeeder proyectoSeeder;

    public DataSeederService(
            UsuarioSeeder usuarioSeeder,
            PatrocinadorSeeder patrocinadorSeeder,
            SystemParameterSeeder systemParameterSeeder,
            ReporteConfigSeeder reporteConfigSeeder,
            ProyectoSeeder proyectoSeeder) {
        this.usuarioSeeder = usuarioSeeder;
        this.patrocinadorSeeder = patrocinadorSeeder;
        this.systemParameterSeeder = systemParameterSeeder;
        this.reporteConfigSeeder = reporteConfigSeeder;
        this.proyectoSeeder = proyectoSeeder;
    }

    /**
     * Ejecuta la carga completa de datos semilla.
     * El orden es importante: dependencias primero, luego entidades relacionadas.
     */
    public void seedAllData() {
        // Orden: Entidades independientes primero, luego las dependientes
        usuarioSeeder.seedUsuarios();
        patrocinadorSeeder.seedPatrocinadores();
        systemParameterSeeder.seedSystemParameters();
        reporteConfigSeeder.seedReporteConfigs();
        proyectoSeeder.seedProyectos();
    }
}
