package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final EntregableRepositoryPort entregableRepositoryPort;
    private final SystemParameterService systemParameterService;

    public DashboardServiceImpl(ProyectoRepositoryPort proyectoRepositoryPort,
                                EntregableRepositoryPort entregableRepositoryPort,
                                SystemParameterService systemParameterService) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.systemParameterService = systemParameterService;
    }

    @Override
    public DashboardSummaryDTO getSummary() {
        long activos = proyectoRepositoryPort.countActivos();
        long cerrados = proyectoRepositoryPort.countCerrados();
        long total = proyectoRepositoryPort.countTotal();

        BigDecimal avgAvance = proyectoRepositoryPort.getAvancePromedio();
        int avancePromedio = 0;
        if (avgAvance != null) {
            avancePromedio = avgAvance.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        LocalDate hoy = LocalDate.now(ZoneId.systemDefault());

        BigDecimal sumaConforme = entregableRepositoryPort.sumPonderacionConformeActivos();
        BigDecimal sumaEsperada = entregableRepositoryPort.sumPonderacionEsperadaActivos(hoy);
        if (sumaConforme == null) sumaConforme = BigDecimal.ZERO;
        if (sumaEsperada == null) sumaEsperada = BigDecimal.ZERO;

        String tendencia = "estable";
        if (sumaEsperada.compareTo(BigDecimal.ZERO) > 0) {
            tendencia = (sumaConforme.compareTo(sumaEsperada) >= 0) ? "positiva" : "negativa";
        }

        long totalAtrasados = entregableRepositoryPort.countAtrasadosTotal(hoy);
        int ventana = systemParameterService.getInt(SystemParameterKeys.DASHBOARD_VENTANA_VENCIMIENTO_DIAS, 7);
        long proximos = entregableRepositoryPort.countProximosActivos(hoy, hoy.plusDays(ventana));

        DashboardSummaryDTO resumen = new DashboardSummaryDTO();
        resumen.setTotalProyectos(total);
        resumen.setActivos(activos);
        resumen.setCerrados(cerrados);
        resumen.setAvancePromedio(avancePromedio);
        resumen.setAvanceTendencia(tendencia);
        resumen.setEntregablesAtrasados(totalAtrasados);
        resumen.setProximosAVencer(proximos);
        resumen.setDiasVentanaVencimiento(ventana);
        return resumen;
    }

    @Override
    public List<DashboardProjectSummaryDTO> getProjectSummary() {
        return proyectoRepositoryPort.getDashboardProjectSummary(LocalDate.now(ZoneId.systemDefault()));
    }

    @Override
    public List<com.proyecta.api_gestion.application.readmodel.ProjectsByDependenciaDTO> getProjectsByDependencia() {
        return proyectoRepositoryPort.getProjectsByDependencia();
    }
}
