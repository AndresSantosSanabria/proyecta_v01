package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.dto.risk.RiesgoCreateDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoUpdateDTO;

import java.util.List;

public interface IRiesgoService {
    List<RiesgoResponseDTO> getRisksByProject(String projectId);
    RiesgoResponseDTO createRisk(RiesgoCreateDTO createDto);
    RiesgoResponseDTO updateRisk(RiesgoUpdateDTO updateDto);
    RiesgoResponseDTO treatRisk(RiesgoTratamientoDTO treatmentDto);
}
