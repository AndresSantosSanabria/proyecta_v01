package com.gobernacion.proyecta.dashboard.domain.port.in;

import com.proyecta.api_gestion.dto.proyecto.DashboardDTO;
import java.util.List;

public interface DashboardUseCase {
    DashboardDTO getSummary();
    List<?> getProjectsByDependencia(); // Simplificado para la demostración
}
