package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.SystemParameterRepository;
import com.proyecta.api_gestion.service.interfaces.DashboardService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProyectoRepository proyectoRepository;
    private final EntregableRepository entregableRepository;
    private final SystemParameterRepository systemParameterRepository;
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    public DashboardServiceImpl(ProyectoRepository proyectoRepository,
            EntregableRepository entregableRepository,
            SystemParameterRepository systemParameterRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregableRepository = entregableRepository;
        this.systemParameterRepository = systemParameterRepository;
    }

    @Override
    public DashboardSummaryDTO getSummary() {
        long activos = proyectoRepository.countActivos();
        long cerrados = proyectoRepository.countCerrados();
        long total = proyectoRepository.countTotal();

        // Avance promedio calculado desde el campo avance_total de cada proyecto
        BigDecimal avgAvance = proyectoRepository.getAvancePromedio();
        int avancePromedio = 0;
        if (avgAvance != null) {
            avancePromedio = avgAvance.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        LocalDate hoy = LocalDate.now();

        // Tendencia: comparar avance real vs lo esperado por fechas
        BigDecimal sumaConforme = entregableRepository.sumPonderacionConformeActivos();
        BigDecimal sumaEsperada = entregableRepository.sumPonderacionEsperadaActivos(hoy);
        if (sumaConforme == null) sumaConforme = BigDecimal.ZERO;
        if (sumaEsperada == null) sumaEsperada = BigDecimal.ZERO;

        String tendencia = "estable";
        if (sumaEsperada.compareTo(BigDecimal.ZERO) > 0) {
            tendencia = (sumaConforme.compareTo(sumaEsperada) >= 0) ? "positiva" : "negativa";
        }

        long totalAtrasados = entregableRepository.countAtrasadosTotal(hoy);

        int ventana = systemParameterRepository.findByKey("dias_alerta_vencimiento")
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValue());
                    } catch (NumberFormatException e) {
                        logger.warn("Formato inválido para parámetro dias_alerta_vencimiento: {}", p.getValue());
                        return 7;
                    }
                })
                .orElse(7);

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
