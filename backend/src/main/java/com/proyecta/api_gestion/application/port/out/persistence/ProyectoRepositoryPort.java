package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.application.readmodel.ProyectoReporteResumenDTO;
import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface ProyectoRepositoryPort {

    PageResult<Proyecto> listarProyectos(String nombre, String codigo, String dependencia,
            com.proyecta.api_gestion.domain.model.enums.EstadoProyecto estado, Boolean peti, PageQuery query);

    java.util.List<Proyecto> findByIdsOrdenadoPorIdDesc(java.util.Collection<String> ids);


    Optional<Proyecto> findById(String id);

    boolean existsByNombreAndDependencia(String nombre, String dependencia);

    long countTotal();

    long countActivos();

    long countCerrados();

    BigDecimal getAvancePromedio();

    long countEntregablesAtrasados(LocalDate hoy);

    long countEntregablesProximosAVencer(LocalDate hoy, LocalDate umbral);

    Optional<Integer> getDiasUmbralProximo();

    Optional<com.proyecta.api_gestion.application.readmodel.ProyectoAvanceDetalleDTO> getAvanceProyecto(String proyectoId);

    List<ProyectoReporteResumenDTO> getProyectosResumen(LocalDate now);

    List<ProyectoReporteResumenDTO> getProyectosConAtrasosResumen(LocalDate now);

    List<DashboardProjectSummaryDTO> getDashboardProjectSummary(LocalDate now);

    List<com.proyecta.api_gestion.application.readmodel.ProjectsByDependenciaDTO> getProjectsByDependencia();

    List<Proyecto> findByDocumentosVerificadosTrueAndRequiereCompletitudDirectorTrueAndCierreForzosoFalse();

    boolean existsDirectorByProyectoIdAndUsername(String proyectoId, String username);

    <E extends Proyecto> E save(E entity);

    boolean existsById(String id);

    java.util.List<Proyecto> findAll();

    java.util.List<Proyecto> findAllById(Iterable<String> ids);

    void delete(Proyecto entity);

    PageResult<Proyecto> findAll(PageQuery query);
}

