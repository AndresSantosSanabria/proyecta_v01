package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final SystemParameterService systemParameterService;

    public DashboardServiceImpl(ProyectoRepository proyectoRepository,
                                EntregableRepository entregableRepository,
                                SystemParameterService systemParameterService) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.systemParameterService = systemParameterService;
    }

    @Override
    public DashboardSummaryDTO getSummary() {
        long activos = proyectoRepository.countActivos();
        long cerrados = proyectoRepository.countCerrados();
        long total = proyectoRepository.countTotal();

        BigDecimal avgAvance = proyectoRepository.getAvancePromedio();
        int avancePromedio = 0;
        if (avgAvance != null) {
            avancePromedio = avgAvance.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        LocalDate hoy = LocalDate.now();

        BigDecimal sumaConforme = entregableRepository.sumPonderacionConformeActivos();
        BigDecimal sumaEsperada = entregableRepository.sumPonderacionEsperadaActivos(hoy);
        if (sumaConforme == null) sumaConforme = BigDecimal.ZERO;
        if (sumaEsperada == null) sumaEsperada = BigDecimal.ZERO;

        String tendencia = "estable";
        if (sumaEsperada.compareTo(BigDecimal.ZERO) > 0) {
            tendencia = (sumaConforme.compareTo(sumaEsperada) >= 0) ? "positiva" : "negativa";
        }

        long totalAtrasados = entregableRepository.countAtrasadosTotal(hoy);
        int ventana = systemParameterService.getInt(SystemParameterKeys.DASHBOARD_VENTANA_VENCIMIENTO_DIAS, 7);
        long proximos = entregableRepository.countProximosActivos(hoy, hoy.plusDays(ventana));

        return new DashboardSummaryDTO(
                total,
                activos,
                cerrados,
                avancePromedio,
                tendencia,
                totalAtrasados,
                proximos,
                ventana);
    }

    @Override
    public List<DashboardProjectSummaryDTO> getProjectSummary() {
        return proyectoRepository.getDashboardProjectSummary(LocalDate.now());
    }

    @Override
    public List<com.proyecta.api_gestion.dto.dashboard.ProjectsByDependenciaDTO> getProjectsByDependencia() {
        return proyectoRepository.getProjectsByDependencia();
    }
}
