package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.RespuestaFurag;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.FuragRespuestaRepository;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final FuragRespuestaRepository furagRespuestaRepository;
    private final IProgressCalculator progressCalculator;

    public ProyectoServiceImpl(ProyectoRepository proyectoRepository,
                               FuragRespuestaRepository furagRespuestaRepository,
                               IProgressCalculator progressCalculator) {
        this.proyectoRepository = proyectoRepository;
        this.furagRespuestaRepository = furagRespuestaRepository;
        this.progressCalculator = progressCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProyectoListDTO> listarProyectos(String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti, Pageable pageable) {
        Specification<Proyecto> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (codigo != null && !codigo.isBlank()) {
                predicates.add(cb.equal(root.get("id"), normalizeProjectId(codigo)));
            }
            if (dependencia != null && !dependencia.isBlank()) {
                predicates.add(cb.equal(root.get("dependencia"), dependencia));
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (peti != null) {
                predicates.add(cb.equal(root.get("peti"), peti));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return proyectoRepository.findAll(spec, pageable).map(this::mapToListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoResponseDTO obtenerPorId(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));
        return mapToResponseDto(proyecto);
    }

    @Override
    @Transactional
    public ProyectoCreatedDTO crearProyecto(ProyectoCreateDTO dto) {
        if (proyectoRepository.existsByNombreAndDependencia(dto.nombre(), dto.dependencia())) {
            throw new BadRequestException("Ya existe un proyecto con ese nombre en la misma dependencia");
        }

        validarPonderaciones(dto.fases());

        Proyecto proyecto = new Proyecto();
        proyecto.setId(generarCodigo());
        proyecto.setNombre(dto.nombre());
        proyecto.setDependencia(dto.dependencia());
        proyecto.setDirector(dto.director());
        proyecto.setCorreoDirector(dto.correoDirector());
        proyecto.setObjetivoGeneral(dto.objetivoGeneral());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setPeti(dto.peti());
        proyecto.setVigenciaPeti(dto.vigenciaPeti());
        proyecto.setEstrategiaPeti(dto.estrategiaPeti());
        proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());

        // Patrocinador
        if (dto.patrocinador() != null) {
            Patrocinador pat = new Patrocinador();
            pat.setNombre(dto.patrocinador().nombre());
            pat.setEntidad(dto.patrocinador().entidad());
            pat.setCargo(dto.patrocinador().cargo());
            pat.setProcesoSigc(dto.patrocinador().procesoSigc());
            pat.setProcedimiento(dto.patrocinador().procedimientoSigc());
            proyecto.setPatrocinador(pat);
        }

        // Equipo
        if (dto.equipoTrabajo() != null) {
            proyecto.setEquipoTrabajo(dto.equipoTrabajo().stream()
                    .map(m -> new MiembroEquipo(m.nombre(), m.cargo(), m.rol()))
                    .collect(Collectors.toList()));
        }

        // Objetivos específicos
        if (dto.objetivosEspecificos() != null) {
            proyecto.setObjetivosEspecificos(dto.objetivosEspecificos().stream()
                    .map(desc -> {
                        ObjetivoEspecifico obj = new ObjetivoEspecifico();
                        obj.setDescripcion(desc);
                        obj.setProyecto(proyecto);
                        return obj;
                    }).collect(Collectors.toList()));
        }

        // FURAG
        if (dto.furag() != null) {
            Furag furag = new Furag();
            furag.setInfraestructuraDatos(dto.furag().infraestructuraDatos());
            furag.setInteroperabilidad(dto.furag().interoperabilidad());
            furag.setDigitalizacionAutomatizacion(dto.furag().digitalizacionAutomatizacion());
            furag.setContratacionPublica(dto.furag().contratacionPublica());
            furag.setServiciosNube(dto.furag().serviciosNube());
            furag.setSandbox(dto.furag().sandbox());
            furag.setTecnologiasEmergentes(dto.furag().tecnologiasEmergentes());
            proyecto.setFurag(furag);
        }

        // Fases / Hitos / Entregables
        proyecto.setFases(dto.fases().stream().map(fDto -> {
            Fase fase = new Fase();
            fase.setNombre(fDto.nombre());
            fase.setDescripcion(fDto.descripcion());
            fase.setPonderacion(BigDecimal.valueOf(fDto.ponderacion()));
            fase.setProyecto(proyecto);
            
            fase.setHitos(fDto.hitos().stream().map(hDto -> {
                Hito hito = new Hito();
                hito.setNombre(hDto.nombre());
                hito.setDescripcion(hDto.descripcion());
                hito.setPonderacion(BigDecimal.valueOf(hDto.ponderacion()));
                hito.setFase(fase);
                
                hito.setEntregables(hDto.entregables().stream().map(eDto -> {
                    if (eDto.fechaLimite().isBefore(dto.fechaInicio())) {
                        throw new BadRequestException("La fecha límite del entregable '" + eDto.nombre() + "' debe ser mayor o igual a la fecha de inicio del proyecto");
                    }
                    Entregable ent = new Entregable();
                    ent.setNombre(eDto.nombre());
                    ent.setPonderacion(BigDecimal.valueOf(eDto.ponderacion()));
                    ent.setFechaLimite(eDto.fechaLimite());
                    ent.setHito(hito);
                    return ent;
                }).collect(Collectors.toList()));
                
                return hito;
            }).collect(Collectors.toList()));
            
            return fase;
        }).collect(Collectors.toList()));

        Proyecto guardado = proyectoRepository.save(proyecto);
        sincronizarRespuestasFurag(guardado);
        return new ProyectoCreatedDTO(guardado.getId(), guardado.getId(), guardado.getNombre(), guardado.getEstado(), "Proyecto creado exitosamente");
    }

    @Override
    @Transactional
    public ProyectoResponseDTO actualizarProyecto(String id, ProyectoUpdateDTO dto) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));

        if (dto.nombre() != null) proyecto.setNombre(dto.nombre());
        if (dto.dependencia() != null) proyecto.setDependencia(dto.dependencia());
        if (dto.director() != null) proyecto.setDirector(dto.director());
        if (dto.correoDirector() != null) proyecto.setCorreoDirector(dto.correoDirector());
        if (dto.objetivoGeneral() != null) proyecto.setObjetivoGeneral(dto.objetivoGeneral());
        if (dto.fechaInicio() != null) proyecto.setFechaInicio(dto.fechaInicio());
        if (dto.peti() != null) proyecto.setPeti(dto.peti());
        if (dto.vigenciaPeti() != null) proyecto.setVigenciaPeti(dto.vigenciaPeti());
        if (dto.estrategiaPeti() != null) proyecto.setEstrategiaPeti(dto.estrategiaPeti());
        if (dto.tienePlanComunicaciones() != null) proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());

        // Actualizar sub-entidades si se proporcionan (omitiendo lógica compleja de merge por brevedad, asumiendo reemplazo)
        // ... (se puede implementar merge fino si es necesario)

        Proyecto actualizado = proyectoRepository.save(proyecto);
        return mapToResponseDto(actualizado);
    }

    @Override
    @Transactional
    public void eliminarProyecto(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));
        proyectoRepository.delete(proyecto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProyectoResumenDTO obtenerResumen(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto p = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));

        long totalFases = p.getFases().size();
        long totalHitos = 0;
        long totalEntregables = 0;
        long entregablesConformes = 0;
        List<String> entregables = new ArrayList<>();

        boolean puedeCerrar = !p.esEstadoTerminal();

        for (Fase f : p.getFases()) {
            totalHitos += f.getHitos().size();
            for (Hito h : f.getHitos()) {
                totalEntregables += h.getEntregables().size();
                entregablesConformes += h.getEntregables().stream()
                        .filter(Entregable::esConforme)
                        .count();
                for (Entregable e : h.getEntregables()) {
                    entregables.add(e.getNombre());
                }

                if (h.getAvanceCalculado() == null || h.getAvanceCalculado().compareTo(new BigDecimal("100")) < 0) {
                    puedeCerrar = false;
                }
                if (!"APROBADO".equalsIgnoreCase(h.getEstadoRevision())) {
                    puedeCerrar = false;
                }
            }
        }

        return new ProyectoResumenDTO(
                p.getId(),
                p.getNombre(),
                p.getDirector(),
                p.getFechaInicio(),
                p.getAvanceTotal(),
                p.getEstadoCodigo(),
                totalFases,
                totalHitos,
                entregablesConformes,
                totalEntregables,
                puedeCerrar,
                entregables
        );
    }

    @Override
    @Transactional
    public void cerrarProyecto(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));

        BigDecimal avanceActual = progressCalculator.calcularYActualizarAvanceProyecto(normalizedId);
        proyecto.setAvanceTotal(avanceActual);

        // Uso de la lógica rica del dominio
        proyecto.cerrar();

        proyectoRepository.save(proyecto);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO obtenerDashboard() {
        LocalDate hoy = LocalDate.now();
        int diasUmbral = proyectoRepository.getDiasUmbralProximo().orElse(8);
        LocalDate umbral = hoy.plusDays(diasUmbral);

        return new DashboardDTO(
                proyectoRepository.countTotal(),
                proyectoRepository.countActivos(),
                proyectoRepository.countCerrados(),
                proyectoRepository.getAvancePromedio(),
                proyectoRepository.countEntregablesAtrasados(hoy),
                proyectoRepository.countEntregablesProximosAVencer(hoy, umbral),
                diasUmbral
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Furag obtenerFurag(String id) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));
        return proyecto.getFurag();
    }

    @Override
    @Transactional
    public void recalcularAvances() {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        for (Proyecto proyecto : proyectos) {
            progressCalculator.calcularYActualizarAvanceProyecto(proyecto.getId());
        }
    }

    @Override
    @Transactional
    public void actualizarFurag(String id, Furag furag) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));
        proyecto.setFurag(furag);
        Proyecto guardado = proyectoRepository.save(proyecto);
        sincronizarRespuestasFurag(guardado);
    }

    private String generarCodigo() {
        long count = proyectoRepository.count() + 1;
        return String.format("IS-PROY-CUN-%03d", count);
    }

    private void validarPonderaciones(List<FaseDTO> fases) {
        int sumFases = fases.stream().mapToInt(FaseDTO::ponderacion).sum();
        if (sumFases <= 0) {
            throw new BadRequestException("La suma de ponderaciones de las fases debe ser mayor a 0");
        }
        for (FaseDTO fase : fases) {
            int sumHitos = fase.hitos().stream().mapToInt(HitoDTO::ponderacion).sum();
            if (sumHitos <= 0) {
                throw new BadRequestException("La suma de ponderaciones de hitos en la fase '" + fase.nombre() + "' debe ser mayor a 0");
            }
            for (HitoDTO hito : fase.hitos()) {
                int sumEntregables = hito.entregables().stream().mapToInt(EntregableDTO::ponderacion).sum();
                if (sumEntregables <= 0) {
                    throw new BadRequestException("La suma de ponderaciones de entregables en el hito '" + hito.nombre() + "' debe ser mayor a 0");
                }
            }
        }
    }

    private ProyectoListDTO mapToListDto(Proyecto p) {
        long total = 0, conformes = 0, atrasados = 0;
        LocalDate hoy = LocalDate.now();
        for (Fase f : p.getFases()) {
            for (Hito h : f.getHitos()) {
                for (Entregable e : h.getEntregables()) {
                    total++;
                    if (e.esConforme()) conformes++;
                    if (!e.esConforme() && e.getFechaLimite() != null && e.getFechaLimite().isBefore(hoy)) atrasados++;
                }
            }
        }

        return new ProyectoListDTO(
                p.getId(),
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                p.getDirector(),
                p.getPeti(),
                p.getAvanceTotal(),
                p.getEstadoCodigo(),
                (int) total,
                (int) conformes,
                (int) atrasados
        );
    }

    private String normalizeProjectId(String id) {
        return id == null ? null : id.trim().toUpperCase(Locale.ROOT);
    }

    private ProyectoResponseDTO mapToResponseDto(Proyecto p) {
        return new ProyectoResponseDTO(
                p.getId(),
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                p.getDirector(),
                p.getCorreoDirector(),
                p.getObjetivoGeneral(),
                p.getObjetivosEspecificos().stream().map(ObjetivoEspecifico::getDescripcion).collect(Collectors.toList()),
                p.getFechaInicio(),
                p.getEstadoCodigo(),
                p.getAvanceTotal(),
                p.getPeti(),
                p.getVigenciaPeti(),
                p.getEstrategiaPetiConfig() != null ? p.getEstrategiaPetiConfig().getCodigo() : null,
                p.getTienePlanComunicaciones(),
                p.getPatrocinador() != null ? new PatrocinadorDTO(p.getPatrocinador().getNombre(), p.getPatrocinador().getEntidad(), p.getPatrocinador().getCargo(), p.getPatrocinador().getProcesoSigc(), p.getPatrocinador().getProcedimiento()) : null,
                p.getEquipoTrabajo().stream().map(m -> new EquipoTrabajoDTO(m.getNombre(), m.getCargo(), m.getRol())).collect(Collectors.toList()),
                p.getFurag() != null ? new FuragDTO(p.getFurag().getInfraestructuraDatos(), p.getFurag().getInteroperabilidad(), p.getFurag().getDigitalizacionAutomatizacion(), p.getFurag().getContratacionPublica(), p.getFurag().getServiciosNube(), p.getFurag().getSandbox(), p.getFurag().getTecnologiasEmergentes()) : null,
                p.getFases().stream().map(f -> new FaseResponseDTO(
                        f.getId(), f.getNombre(), f.getDescripcion(), f.getPonderacion(), f.getAvanceCalculado(),
                        f.getHitos().stream().map(h -> new HitoResponseDTO(
                                h.getId(), h.getNombre(), h.getDescripcion(), h.getPonderacion(), h.getAvanceCalculado(),
                                h.getEntregables().stream().map(e -> new EntregableResponseDTO(
                                        e.getId(), e.getNombre(), e.getPonderacion(), e.getEstadoCodigo(), e.esConforme(), e.getFechaLimite()
                                )).collect(Collectors.toList())
                        )).collect(Collectors.toList())
                )).collect(Collectors.toList())
        );
    }

    private void sincronizarRespuestasFurag(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return;
        }

        furagRespuestaRepository.deleteByProyecto_Id(proyecto.getId());

        Furag furag = proyecto.getFurag();
        if (furag == null) {
            return;
        }

        furagRespuestaRepository.saveAll(List.of(
                buildFuragRespuesta(proyecto, "FURAG_INFRAESTRUCTURA_DATOS", "Infraestructura de datos", furag.getInfraestructuraDatos()),
                buildFuragRespuesta(proyecto, "FURAG_INTEROPERABILIDAD", "Interoperabilidad", furag.getInteroperabilidad()),
                buildFuragRespuesta(proyecto, "FURAG_DIGITALIZACION_AUTOMATIZACION", "Digitalización y automatización", furag.getDigitalizacionAutomatizacion()),
                buildFuragRespuesta(proyecto, "FURAG_CONTRATACION_PUBLICA", "Contratación pública", furag.getContratacionPublica()),
                buildFuragRespuesta(proyecto, "FURAG_SERVICIOS_NUBE", "Servicios en nube", furag.getServiciosNube()),
                buildFuragRespuesta(proyecto, "FURAG_SANDBOX", "Sandbox", furag.getSandbox()),
                buildFuragRespuesta(proyecto, "FURAG_TECNOLOGIAS_EMERGENTES", "Tecnologías emergentes", furag.getTecnologiasEmergentes())
        ));
    }

    private FuragRespuesta buildFuragRespuesta(Proyecto proyecto, String codigo, String pregunta, RespuestaFurag respuesta) {
        FuragRespuesta item = new FuragRespuesta();
        item.setProyecto(proyecto);
        item.setCodigoPregunta(codigo);
        item.setPregunta(pregunta);
        item.setRespuesta(respuesta);
        item.setObligatoria(true);
        return item;
    }
}
