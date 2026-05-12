package com.gobernacion.proyecta.entregables.infrastructure;

import com.gobernacion.proyecta.entregables.domain.port.out.EntregableRepositoryPort;
import com.gobernacion.proyecta.entregables.domain.service.EntregableService;
import com.gobernacion.proyecta.entregables.domain.port.in.EntregableUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntregableConfig {

    @Bean
    public EntregableUseCase entregableUseCase(EntregableRepositoryPort repositoryPort) {
        return new EntregableService(repositoryPort);
    }
}
