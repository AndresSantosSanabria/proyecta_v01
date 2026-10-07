package com.proyecta.api_gestion.application.port.out.persistence;

import com.proyecta.api_gestion.application.readmodel.EntregablePendienteDTO;
import com.proyecta.api_gestion.domain.model.Entregable;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;

public interface EntregableRepositoryPort {

    List<Entregable> findByHitoId(Integer hitoId);

    long countAtrasadosTotal(LocalDate hoy);

    long countProximosActivos(LocalDate hoy, LocalDate fin);

    BigDecimal sumPonderacionConformeActivos();

    BigDecimal sumPonderacionEsperadaActivos(LocalDate hoy);

    List<EntregablePendienteDTO> findPendientesVencidosByProyecto(String proyectoId, LocalDate hoy);

    List<Entregable> findByProyectoId(String proyectoId);

    List<Entregable> findNoCompletados(LocalDate hoy);

    List<Entregable> findVencidosNoEntregados(LocalDate hoy);

    Optional<Entregable> findByIdAndProyectoIdWithHierarchy(Integer entregableId, String proyectoId);

    <E extends Entregable> E save(E entity);

    java.util.Optional<Entregable> findById(Integer id);
}

