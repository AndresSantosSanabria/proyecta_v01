package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.model.Furag;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReporteServiceImpl implements ReporteService {

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final EntregableRepository entregableRepository;
    private final RiesgoRepository riesgoRepository;
    private final ReporteConfigRepository reporteConfigRepository;
    private final SystemParameterService systemParameterService;

    public ReporteServiceImpl(ProyectoRepository proyectoRepository,
                              FaseRepository faseRepository,
                              EntregableRepository entregableRepository,
                              RiesgoRepository riesgoRepository,
                              ReporteConfigRepository reporteConfigRepository,
                              SystemParameterService systemParameterService) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.entregableRepository = entregableRepository;
        this.riesgoRepository = riesgoRepository;
        this.reporteConfigRepository = reporteConfigRepository;
        this.systemParameterService = systemParameterService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteConfigDTO> obtenerConfiguracionReportes() {
        return reporteConfigRepository.findAllByActivoTrueOrderByOrdenAsc().stream()
                .map(c -> new ReporteConfigDTO(c.getId(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReporteVistaPreviaDTO> obtenerVistaPrevia(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(proyecto -> {
            BigDecimal avancePromedio = faseRepository.getAvancePromedioByProyecto(proyectoId);
            if (avancePromedio == null) avancePromedio = BigDecimal.ZERO;

            avancePromedio = avancePromedio.setScale(2, RoundingMode.HALF_UP);

            List<EntregablePendienteDTO> entregablesVencidos =
                    entregableRepository.findPendientesVencidosByProyecto(proyectoId, LocalDate.now());

            String directorNombre = proyecto.getDirector() != null ? proyecto.getDirector() : "No asignado";
            String patrocinadorNombre = proyecto.getPatrocinador() != null ? proyecto.getPatrocinador().getNombre() : "No asignado";

            return new ReporteVistaPreviaDTO(
                    proyecto.getId(),
                    proyecto.getNombre(),
                    avancePromedio,
                    directorNombre,
                    proyecto.getDependencia(),
                    patrocinadorNombre,
                    entregablesVencidos
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerTodosLosProyectos() {
        return proyectoRepository.getProyectosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProyectoReporteResumenDTO> obtenerProyectosConRetrasos() {
        return proyectoRepository.getProyectosConAtrasosResumen(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanComunicacionesDTO> obtenerPlanComunicaciones(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p ->
                new PlanComunicacionesDTO(p.getId(), p.getNombre(), p.getPlanComunicacionesPdf(), p.getDependencia())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuragReporteDTO> obtenerFurag(String proyectoId) {
        return proyectoRepository.findById(proyectoId).map(p ->
                {
                    validarFuragObligatorio(p.getFurag(), proyectoId);
                    return new FuragReporteDTO(
                            p.getId(),
                            p.getNombre(),
                            p.getPeti(),
                            p.getEstrategiaPeti() != null ? p.getEstrategiaPeti().name() : null,
                            p.getVigenciaPeti(),
                            p.getObjetivoGeneral()
                    );
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiesgoReporteDTO> obtenerRiesgos(String proyectoId) {
        return riesgoRepository.findRiesgosReporteByProyecto(proyectoId);
    }

    @Override
    public byte[] generarReporteProyectoPdf(String id) {
        // Simulación de generación de PDF
        return "Contenido PDF simulado para proyecto".getBytes();
    }

    @Override
    public byte[] generarReportePortafolioPdf() {
        return "Contenido PDF simulado para portafolio".getBytes();
    }

    @Override
    public byte[] generarReportePortafolioExcel() {
        return "Contenido Excel simulado para portafolio".getBytes();
    }

    private void validarFuragObligatorio(Furag furag, String proyectoId) {
        if (furag == null) {
            throw new ForbiddenException("El proyecto " + proyectoId + " no tiene respuestas FURAG suficientes para generar el reporte.");
        }

        int minimoRespuestas = systemParameterService.getInt(SystemParameterKeys.FURAG_RESPUESTAS_OBLIGATORIAS, 7);
        long respuestasCompletas = 0;
        if (furag.getInfraestructuraDatos() != null) respuestasCompletas++;
        if (furag.getInteroperabilidad() != null) respuestasCompletas++;
        if (furag.getDigitalizacionAutomatizacion() != null) respuestasCompletas++;
        if (furag.getContratacionPublica() != null) respuestasCompletas++;
        if (furag.getServiciosNube() != null) respuestasCompletas++;
        if (furag.getSandbox() != null) respuestasCompletas++;
        if (furag.getTecnologiasEmergentes() != null) respuestasCompletas++;

        if (respuestasCompletas < minimoRespuestas) {
            throw new ForbiddenException("El proyecto " + proyectoId + " no tiene completas las respuestas FURAG obligatorias para generar el reporte.");
        }
    }
}
