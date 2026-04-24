package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.dto.DashboardDto.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.DashboardDto.DashboardSummaryDTO;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.SystemParameterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EntregableRepository entregableRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    public DashboardSummaryDTO getSummary() {
        long activos = proyectoRepository.countByEstado(EstadoProyecto.activo);
        long cerrados = proyectoRepository.countByEstado(EstadoProyecto.cerrado);
        long total = activos + cerrados;

        BigDecimal sumaConforme = entregableRepository.sumPonderacionConformeActivos();
        BigDecimal sumaTotal = entregableRepository.sumTotalPonderacionActivos();

        if (sumaConforme == null)
            sumaConforme = BigDecimal.ZERO;
        if (sumaTotal == null)
            sumaTotal = BigDecimal.ZERO;

        int avancePromedio = 0;
        if (sumaTotal.compareTo(BigDecimal.ZERO) > 0) {
            avancePromedio = sumaConforme.multiply(new BigDecimal("100"))
                    .divide(sumaTotal, 0, RoundingMode.HALF_UP).intValue();
        }

        LocalDate hoy = LocalDate.now();

        // Tendencia basada en cumplimiento de cronograma real
        BigDecimal sumaEsperada = entregableRepository.sumPonderacionEsperadaActivos(hoy);
        if (sumaEsperada == null)
            sumaEsperada = BigDecimal.ZERO;

        String tendencia = "estable";
        if (sumaEsperada.compareTo(BigDecimal.ZERO) > 0) {
            tendencia = (sumaConforme.compareTo(sumaEsperada) >= 0) ? "positiva" : "negativa";
        }

        // Total de entregables atrasados en toda la plataforma
        long totalAtrasados = entregableRepository.countAtrasadosActivos(hoy);

        // Obtener ventana de vencimiento desde BD o usar default
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

        // Entregables próximos a vencer (ventana técnica configurada para el cálculo)
        long proximos = entregableRepository.countProximosActivos(hoy, hoy.plusDays(ventana));

        return new DashboardSummaryDTO(
                total,
                activos,
                cerrados,
                avancePromedio,
                tendencia,
                totalAtrasados,
                proximos,
                ventana
        );
    }
    
    public List<DashboardProjectSummaryDTO> getProjectSummary() {
        // Enviamos la fecha actual para que la query calcule qué está atrasado hoy
        return proyectoRepository.getDashboardProjectSummary(LocalDate.now());
    }
}
