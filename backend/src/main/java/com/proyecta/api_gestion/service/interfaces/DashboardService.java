package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;

import java.util.List;

public interface DashboardService {
    DashboardSummaryDTO getSummary();
    List<DashboardProjectSummaryDTO> getProjectSummary();
}
