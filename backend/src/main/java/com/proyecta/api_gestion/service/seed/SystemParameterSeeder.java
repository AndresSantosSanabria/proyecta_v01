package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.SystemParameter;
import com.proyecta.api_gestion.repository.SystemParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Seeder especializado para parámetros del sistema.
 * 
 * Responsabilidad: Inicializar parámetros de configuración global (SRP).
 */
@Service
public class SystemParameterSeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemParameterSeeder.class);
    private final SystemParameterRepository parameterRepository;

    public SystemParameterSeeder(SystemParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    public void seedSystemParameters() {
        logger.info("Cargando parámetros del sistema...");
        
        crearParametroSiNoExiste(
            "ventana_vencimiento_dias",
            "8",
            "Días ventana de vencimiento para alertas de proyectos"
        );
        
        crearParametroSiNoExiste(
            "avance_critico_porcentaje",
            "50",
            "Porcentaje mínimo de avance para considerar proyecto en riesgo"
        );
        
        crearParametroSiNoExiste(
            "max_proyectos_activos",
            "20",
            "Máximo número de proyectos activos simultáneos"
        );
        
        logger.info("✓ Parámetros del sistema cargados");
    }

    private void crearParametroSiNoExiste(String clave, String valor, String descripcion) {
        if (!parameterRepository.existsById(clave)) {
            SystemParameter param = new SystemParameter(clave, valor, descripcion);
            parameterRepository.save(param);
            logger.debug("Parámetro creado: {}", clave);
        }
    }
}
