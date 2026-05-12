package com.gobernacion.proyecta.proyectos.infrastructure;

import com.gobernacion.proyecta.proyectos.domain.port.out.ProyectoRepositoryPort;
import com.gobernacion.proyecta.proyectos.domain.service.ProyectoService;
import com.gobernacion.proyecta.proyectos.domain.port.in.ProyectoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProyectoConfig {

    @Bean
    public ProyectoUseCase proyectoUseCase(ProyectoRepositoryPort repositoryPort) {
        return new ProyectoService(repositoryPort);
    }
}
