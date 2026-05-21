package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.Patrocinador;
import com.proyecta.api_gestion.repository.PatrocinadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Seeder especializado para la entidad Patrocinador.
 * 
 * Responsabilidad: Solo crear patrocinadores (SRP).
 * Utilidad: Proporciona métodos para obtener patrocinadores ya creados.
 */
@Service
public class PatrocinadorSeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(PatrocinadorSeeder.class);
    private final PatrocinadorRepository patrocinadorRepository;

    public PatrocinadorSeeder(PatrocinadorRepository patrocinadorRepository) {
        this.patrocinadorRepository = patrocinadorRepository;
    }

    public void seedPatrocinadores() {
        logger.info("Cargando patrocinadores semilla...");
        
        crearPatrocinadorSiNoExiste(
            "Secretaría de TIC",
            "Secretario",
            "Despacho",
            "Gobernación de Cundinamarca",
            "PROC-TIC",
            "Procesos TIC"
        );
        
        crearPatrocinadorSiNoExiste(
            "Dirección de Infraestructura",
            "Director TIC",
            "TIC",
            "Gobernación de Cundinamarca",
            "INFRA-001",
            "Infraestructura TIC"
        );
        
        logger.info("✓ Patrocinadores semilla cargados");
    }

    private void crearPatrocinadorSiNoExiste(String nombre, String cargo, String dependencia,
                                             String entidad, String procesoSigc, String procedimiento) {
        if (patrocinadorRepository.findByNombre(nombre).isEmpty()) {
            Patrocinador p = new Patrocinador();
            p.setNombre(nombre);
            p.setCargo(cargo);
            p.setDependencia(dependencia);
            p.setEntidad(entidad);
            p.setProcesoSigc(procesoSigc);
            p.setProcedimiento(procedimiento);
            patrocinadorRepository.save(p);
            logger.debug("Patrocinador creado: {}", nombre);
        }
    }

    public Patrocinador obtenerPatrocinador(String nombre) {
        return patrocinadorRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("Patrocinador no encontrado: " + nombre));
    }
}
