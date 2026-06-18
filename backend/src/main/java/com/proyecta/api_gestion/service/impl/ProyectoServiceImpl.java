package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.config.EstrategiaPetiConfig;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
import com.proyecta.api_gestion.model.enums.RespuestaFurag;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.FuragRespuestaRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import com.proyecta.api_gestion.service.interfaces.IProgressCalculator;
import com.proyecta.api_gestion.service.config.PetiCatalogService;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.security.dynamic.SecurityCatalogCacheService;
import com.proyecta.api_gestion.service.support.ProjectHierarchyOrdering;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProyectoServiceImpl implements ProyectoService {

    private static final String PROJECT_CODE_PREFIX = "IS-PROY-CUN-";
    private static final String PROJECT_CODE_REGEX = "^IS-PROY-CUN-\\d+$";

    private final ProyectoRepository proyectoRepository;
    private final FuragRespuestaRepository furagRespuestaRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final SeguridadUsuarioRepository seguridadUsuarioRepository;
    private final SecurityCatalogCacheService securityCatalogCacheService;
    private final PetiCatalogService petiCatalogService;
    private final IProgressCalculator progressCalculator;
    private final NotificationEventPublisherPort notificationPublisher;

    public ProyectoServiceImpl(ProyectoRepository proyectoRepository,
                               FuragRespuestaRepository furagRespuestaRepository,
                               SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
                               SeguridadUsuarioRepository seguridadUsuarioRepository,
                               SecurityCatalogCacheService securityCatalogCacheService,
                               PetiCatalogService petiCatalogService,
                               IProgressCalculator progressCalculator,
                               NotificationEventPublisherPort notificationPublisher) {
        this.proyectoRepository = proyectoRepository;
        this.furagRespuestaRepository = furagRespuestaRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.seguridadUsuarioRepository = seguridadUsuarioRepository;
        this.securityCatalogCacheService = securityCatalogCacheService;
        this.petiCatalogService = petiCatalogService;
        this.progressCalculator = progressCalculator;
        this.notificationPublisher = notificationPublisher;
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
    public List<ProyectoListDTO> listarProyectosAsignados(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<String> proyectoIds = usuarioProyectoRepository.findProyectoIdsByUsername(username).stream()
                .map(this::normalizeProjectId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        if (proyectoIds.isEmpty()) {
            return List.of();
        }

        Specification<Proyecto> spec = (root, query, cb) -> root.get("id").in(proyectoIds);
        return proyectoRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
                .map(this::mapToListDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeguridadUsuarioDTO> listarDirectoresAsignables() {
        return seguridadUsuarioRepository.findAssignableProjectDirectors().stream()
                .map(this::mapToSeguridadUsuarioDto)
                .toList();
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
        aplicarEstrategiaPeti(proyecto, dto.estrategiaPeti());
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

        // Fases / Hitos / Entregables. La jerarquia inicial es opcional.
        List<FaseDTO> fasesDto = dto.fases() == null ? List.of() : dto.fases();
        if (!fasesDto.isEmpty()) {
            proyecto.setFases(fasesDto.stream().map(fDto -> {
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
                    validarFechasEntregableNuevo(eDto, dto.fechaInicio());
                    Entregable ent = new Entregable();
                    ent.setNombre(eDto.nombre());
                    ent.setPonderacion(BigDecimal.valueOf(eDto.ponderacion()));
                    ent.setFechaInicio(eDto.fechaInicio());
                    ent.setFechaLimite(eDto.fechaLimite());
                    ent.setHito(hito);
                    return ent;
                }).collect(Collectors.toList()));
                
                return hito;
            }).collect(Collectors.toList()));
            
            return fase;
        }).collect(Collectors.toList()));
        }

        Proyecto guardado = proyectoRepository.save(proyecto);
        sincronizarRespuestasFurag(guardado);
        return new ProyectoCreatedDTO(guardado.getId(), guardado.getId(), guardado.getNombre(), guardado.getEstado(), "Proyecto creado exitosamente");
    }

    @Override
    @Transactional
    public ProyectoCreatedDTO registrarProyectoInicial(ProyectoRegistroInicialDTO dto, String gestorUsername) {
        String codigoSolicitado = trimToNull(dto.codigoProyecto());
        String codigo = codigoSolicitado == null ? generarCodigo() : normalizeProjectId(codigoSolicitado);
        if (!codigo.matches("[A-Z0-9][A-Z0-9_-]*")) {
            throw new BadRequestException("El codigo del proyecto solo puede contener letras, numeros, guiones y guiones bajos");
        }
        if (proyectoRepository.existsById(codigo)) {
            if (esCodigoAutomatico(codigo)) {
                codigo = generarCodigo();
            } else {
                throw new BadRequestException("Ya existe un proyecto con el codigo: " + codigo);
            }
        }

        SeguridadUsuario director = seguridadUsuarioRepository.findById(dto.directorUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario Director no encontrado con id: " + dto.directorUsuarioId()));
        validarUsuarioDirector(director);

        Proyecto proyecto = new Proyecto();
        proyecto.setId(codigo);
        proyecto.setNombre(dto.nombre().trim());
        proyecto.setObjetivoGeneral(dto.objetivoGeneral().trim());
        proyecto.setDirector(firstNonBlank(director.getNombre(), director.getUsername()));
        proyecto.setCorreoDirector(director.getCorreo());
        proyecto.setAvanceTotal(BigDecimal.ZERO);
        proyecto.marcarRegistroInicialPendiente(gestorUsername);

        Proyecto guardado = proyectoRepository.save(proyecto);
        asignarDirectorProyecto(guardado, director);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_INITIAL_REGISTERED,
                guardado.getId(),
                gestorUsername,
                Map.of(
                        "projectName", guardado.getNombre(),
                        "state", guardado.getEstadoCodigo(),
                        "recipients", List.of(director.getCorreo(), gestorUsername)
                )));

        return new ProyectoCreatedDTO(
                guardado.getId(),
                guardado.getId(),
                guardado.getNombre(),
                guardado.getEstado(),
                "Proyecto registrado. Pendiente de completar por el Director asignado."
        );
    }

    @Override
    @Transactional
    public ProyectoCompletionStatusDTO obtenerEstadoCompletitud(String id, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));

        boolean directorAsignado = esDirectorAsignado(proyecto, username);
        if (proyecto.requiereCompletitudDirector() && directorAsignado) {
            proyecto.registrarPrimerIngresoDirector();
            proyecto = proyectoRepository.save(proyecto);
        }

        boolean requiereCompletitud = proyecto.requiereCompletitudDirector();
        boolean puedeCompletar = requiereCompletitud && directorAsignado;
        String mensaje = requiereCompletitud
                ? "El Director asignado debe completar la informacion inicial antes de acceder a los modulos operativos."
                : "El proyecto ya tiene su informacion inicial completa.";

        return new ProyectoCompletionStatusDTO(
                proyecto.getId(),
                proyecto.getEstadoCodigo(),
                requiereCompletitud,
                proyecto.getPrimerIngresoDirectorAt() != null,
                puedeCompletar,
                proyecto.getPrimerIngresoDirectorAt(),
                proyecto.getCompletadoPorDirectorAt(),
                mensaje
        );
    }

    @Override
    @Transactional
    public ProyectoResponseDTO completarInformacionInicial(String id, ProyectoCompletarInformacionDTO dto, String username) {
        final String normalizedId = normalizeProjectId(id);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + normalizedId));

        if (!proyecto.requiereCompletitudDirector()) {
            throw new BadRequestException("El proyecto no esta pendiente de completar.");
        }
        if (!esDirectorAsignado(proyecto, username)) {
            throw new BadRequestException("Solo el Director asignado puede completar la informacion inicial del proyecto.");
        }

        aplicarInformacionComplementaria(proyecto, dto);
        proyecto.completarInformacionInicialPorDirector();

        Proyecto guardado = proyectoRepository.save(proyecto);
        sincronizarRespuestasFurag(guardado);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_INITIAL_COMPLETED,
                guardado.getId(),
                username,
                Map.of(
                        "projectName", guardado.getNombre(),
                        "state", guardado.getEstadoCodigo(),
                        "recipients", List.of(guardado.getCorreoDirector(), guardado.getRegistradoInicialPor())
                )));
        return mapToResponseDto(guardado);
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
        if (dto.estrategiaPeti() != null) aplicarEstrategiaPeti(proyecto, dto.estrategiaPeti());
        if (dto.tienePlanComunicaciones() != null) proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());

        // Actualizar sub-entidades si se proporcionan (omitiendo lógica compleja de merge por brevedad, asumiendo reemplazo)
        // ... (se puede implementar merge fino si es necesario)

        Proyecto actualizado = proyectoRepository.save(proyecto);
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_UPDATED,
                actualizado.getId(),
                "system",
                Map.of(
                        "projectName", actualizado.getNombre(),
                        "state", actualizado.getEstadoCodigo(),
                        "recipients", List.of(actualizado.getCorreoDirector())
                )));
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
        String patrocinadorNombre = p.getPatrocinador() != null ? p.getPatrocinador().getNombre() : null;
        String patrocinadorCargo = p.getPatrocinador() != null ? p.getPatrocinador().getCargo() : null;
        String patrocinadorEntidad = p.getPatrocinador() != null ? p.getPatrocinador().getEntidad() : null;
        SeguridadUsuarioProyecto directorAsignado = findDirectorAsignado(p);
        String directorNombre = directorNameFromAssignment(directorAsignado);
        String directorCargo = directorNombre != null ? directorAsignado.getCargo() : null;
        String directorEntidad = directorNombre != null && directorAsignado.getUsuario() != null
                ? directorAsignado.getUsuario().getDependencia()
                : null;

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
            }
        }

        if (totalEntregables == 0 || entregablesConformes < totalEntregables) {
            puedeCerrar = false;
        }

        return new ProyectoResumenDTO(
                p.getId(),
                p.getNombre(),
                directorNombre,
                directorCargo,
                directorEntidad,
                patrocinadorNombre,
                patrocinadorCargo,
                patrocinadorEntidad,
                p.getFechaInicio(),
                p.getObjetivoGeneral(),
                p.getObjetivosEspecificos() == null ? List.of() : p.getObjetivosEspecificos().stream()
                        .map(ObjetivoEspecifico::getDescripcion)
                        .filter(value -> value != null && !value.isBlank())
                        .toList(),
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
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_CLOSED,
                proyecto.getId(),
                "system",
                Map.of(
                        "projectName", proyecto.getNombre(),
                        "state", proyecto.getEstadoCodigo(),
                        "recipients", List.of(proyecto.getCorreoDirector())
                )));
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

    private void aplicarInformacionComplementaria(Proyecto proyecto, ProyectoCompletarInformacionDTO dto) {
        validarPonderaciones(dto.fases());

        proyecto.setDependencia(dto.dependencia().trim());
        proyecto.setFechaInicio(dto.fechaInicio());
        proyecto.setAlcanceDetallado(trimToNull(dto.alcanceDetallado()));
        proyecto.setPresupuestoEstimado(dto.presupuestoEstimado());
        proyecto.setPeti(dto.peti());
        proyecto.setVigenciaPeti(trimToNull(dto.vigenciaPeti()));
        aplicarEstrategiaPeti(proyecto, dto.estrategiaPeti());
        proyecto.setTienePlanComunicaciones(dto.tienePlanComunicaciones());
        proyecto.setPatrocinador(buildPatrocinador(dto.patrocinador()));
        proyecto.setFurag(buildFurag(dto.furag()));

        proyecto.getObjetivosEspecificos().clear();
        if (dto.objetivosEspecificos() != null) {
            dto.objetivosEspecificos().stream()
                    .map(this::trimToNull)
                    .filter(value -> value != null && !value.isBlank())
                    .map(desc -> {
                        ObjetivoEspecifico obj = new ObjetivoEspecifico();
                        obj.setDescripcion(desc);
                        obj.setProyecto(proyecto);
                        return obj;
                    })
                    .forEach(proyecto.getObjetivosEspecificos()::add);
        }

        proyecto.getEquipoTrabajo().clear();
        if (dto.equipoTrabajo() != null) {
            dto.equipoTrabajo().stream()
                    .map(m -> new MiembroEquipo(m.nombre(), m.cargo(), m.rol()))
                    .forEach(proyecto.getEquipoTrabajo()::add);
        }

        if (dto.fases() != null) {
            proyecto.getFases().clear();
            dto.fases().stream()
                    .map(faseDto -> buildFase(faseDto, proyecto, dto.fechaInicio()))
                    .forEach(proyecto.getFases()::add);
        }
    }

    private Patrocinador buildPatrocinador(PatrocinadorDTO dto) {
        if (dto == null) {
            return null;
        }
        Patrocinador pat = new Patrocinador();
        pat.setNombre(dto.nombre());
        pat.setEntidad(dto.entidad());
        pat.setCargo(dto.cargo());
        pat.setProcesoSigc(dto.procesoSigc());
        pat.setProcedimiento(dto.procedimientoSigc());
        return pat;
    }

    private Furag buildFurag(FuragDTO dto) {
        if (dto == null) {
            return null;
        }
        Furag furag = new Furag();
        furag.setInfraestructuraDatos(dto.infraestructuraDatos());
        furag.setInteroperabilidad(dto.interoperabilidad());
        furag.setDigitalizacionAutomatizacion(dto.digitalizacionAutomatizacion());
        furag.setContratacionPublica(dto.contratacionPublica());
        furag.setServiciosNube(dto.serviciosNube());
        furag.setSandbox(dto.sandbox());
        furag.setTecnologiasEmergentes(dto.tecnologiasEmergentes());
        return furag;
    }

    private Fase buildFase(FaseDTO fDto, Proyecto proyecto, LocalDate fechaInicioProyecto) {
        Fase fase = new Fase();
        fase.setNombre(fDto.nombre());
        fase.setDescripcion(fDto.descripcion());
        fase.setPonderacion(BigDecimal.valueOf(fDto.ponderacion()));
        fase.setProyecto(proyecto);

        List<Hito> hitos = fDto.hitos().stream()
                .map(hDto -> buildHito(hDto, fase, fechaInicioProyecto))
                .collect(Collectors.toList());
        fase.setHitos(hitos);
        return fase;
    }

    private Hito buildHito(HitoDTO hDto, Fase fase, LocalDate fechaInicioProyecto) {
        Hito hito = new Hito();
        hito.setNombre(hDto.nombre());
        hito.setDescripcion(hDto.descripcion());
        hito.setPonderacion(BigDecimal.valueOf(hDto.ponderacion()));
        hito.setFase(fase);

        List<Entregable> entregables = hDto.entregables().stream()
                .map(eDto -> buildEntregable(eDto, hito, fechaInicioProyecto))
                .collect(Collectors.toList());
        hito.setEntregables(entregables);
        return hito;
    }

    private Entregable buildEntregable(EntregableDTO eDto, Hito hito, LocalDate fechaInicioProyecto) {
        validarFechasEntregableNuevo(eDto, fechaInicioProyecto);
        Entregable ent = new Entregable();
        ent.setNombre(eDto.nombre());
        ent.setPonderacion(BigDecimal.valueOf(eDto.ponderacion()));
        ent.setFechaInicio(eDto.fechaInicio());
        ent.setFechaLimite(eDto.fechaLimite());
        ent.setHito(hito);
        return ent;
    }

    private void validarUsuarioDirector(SeguridadUsuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("El usuario Director asignado no esta activo.");
        }
        if (!esRolDirector(usuario)) {
            throw new BadRequestException("El usuario asignado debe tener rol Director de Proyecto.");
        }
    }

    private boolean esRolDirector(SeguridadUsuario usuario) {
        Set<String> directorRoles = Set.of("director_proyecto", "director_pro");
        String rolCodigo = normalizeRole(usuario.getRolCodigo());
        String rolNombre = normalizeRole(usuario.getRolNombre());
        return directorRoles.contains(rolCodigo) || directorRoles.contains(rolNombre) || rolNombre.contains("director");
    }

    private void asignarDirectorProyecto(Proyecto proyecto, SeguridadUsuario director) {
        SeguridadUsuarioProyecto asignacion = usuarioProyectoRepository
                .findByUsuario_UsernameIgnoreCaseAndProyectoIdIgnoreCaseAndCargoIgnoreCase(
                        director.getUsername(),
                        proyecto.getId(),
                        "DIRECTOR_PROYECTO"
                )
                .orElseGet(SeguridadUsuarioProyecto::new);
        asignacion.setUsuario(director);
        asignacion.setProyectoId(proyecto.getId());
        asignacion.setCargo("DIRECTOR_PROYECTO");
        asignacion.setActivo(true);
        usuarioProyectoRepository.save(asignacion);
        securityCatalogCacheService.evictAll();
    }

    private boolean esDirectorAsignado(Proyecto proyecto, String username) {
        if (proyecto == null || proyecto.getId() == null || username == null || username.isBlank()) {
            return false;
        }
        return usuarioProyectoRepository.findActiveDirectorAssignmentsByProyectoId(proyecto.getId()).stream()
                .map(SeguridadUsuarioProyecto::getUsuario)
                .filter(usuario -> usuario != null && usuario.getUsername() != null)
                .anyMatch(usuario -> usuario.getUsername().equalsIgnoreCase(username));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private SeguridadUsuarioDTO mapToSeguridadUsuarioDto(SeguridadUsuario usuario) {
        return new SeguridadUsuarioDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getDependencia(),
                usuario.getActivo(),
                usuario.getRolCodigo(),
                usuario.getRolNombre(),
                usuario.getFechaCreacion(),
                usuario.getUltimoAcceso()
        );
    }

    private synchronized String generarCodigo() {
        int maxConsecutivo = proyectoRepository.findAll().stream()
                .map(Proyecto::getId)
                .mapToInt(this::extractConsecutivoCodigoProyecto)
                .max()
                .orElse(0);

        String codigo;
        int siguiente = maxConsecutivo + 1;
        do {
            codigo = String.format("%s%03d", PROJECT_CODE_PREFIX, siguiente++);
        } while (proyectoRepository.existsById(codigo));

        return codigo;
    }

    private int extractConsecutivoCodigoProyecto(String id) {
        String normalized = normalizeProjectId(id);
        if (normalized == null || !normalized.startsWith(PROJECT_CODE_PREFIX)) {
            return 0;
        }

        try {
            return Integer.parseInt(normalized.substring(PROJECT_CODE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean esCodigoAutomatico(String codigo) {
        return codigo != null && codigo.matches(PROJECT_CODE_REGEX);
    }

    private void validarPonderaciones(List<FaseDTO> fases) {
        if (fases == null || fases.isEmpty()) {
            return;
        }

        int sumFases = 0;
        for (FaseDTO fase : fases) {
            if (fase.nombre() == null || fase.nombre().isBlank()) {
                throw new BadRequestException("El nombre de la fase es obligatorio");
            }
            if (fase.ponderacion() == null || fase.ponderacion() < 1 || fase.ponderacion() > 100) {
                throw new BadRequestException("La ponderacion de la fase '" + fase.nombre() + "' debe estar entre 1 y 100");
            }
            sumFases += fase.ponderacion();
        }
        if (sumFases != 100) {
            throw new BadRequestException("La suma de ponderaciones de las fases debe ser exactamente 100");
        }

        for (FaseDTO fase : fases) {
            if (fase.hitos() == null || fase.hitos().isEmpty()) {
                throw new BadRequestException("La fase '" + fase.nombre() + "' debe tener al menos un hito");
            }

            int sumHitos = 0;
            for (HitoDTO hito : fase.hitos()) {
                if (hito.nombre() == null || hito.nombre().isBlank()) {
                    throw new BadRequestException("El nombre del hito es obligatorio");
                }
                if (hito.ponderacion() == null || hito.ponderacion() < 1 || hito.ponderacion() > 100) {
                    throw new BadRequestException("La ponderacion del hito '" + hito.nombre() + "' debe estar entre 1 y 100");
                }
                sumHitos += hito.ponderacion();
            }
            if (sumHitos != 100) {
                throw new BadRequestException("La suma de ponderaciones de hitos en la fase '" + fase.nombre() + "' debe ser exactamente 100");
            }

            for (HitoDTO hito : fase.hitos()) {
                if (hito.entregables() == null || hito.entregables().isEmpty()) {
                    throw new BadRequestException("El hito '" + hito.nombre() + "' debe tener al menos un entregable");
                }

                int sumEntregables = 0;
                for (EntregableDTO entregable : hito.entregables()) {
                    if (entregable.nombre() == null || entregable.nombre().isBlank()) {
                        throw new BadRequestException("El nombre del entregable es obligatorio");
                    }
                    if (entregable.ponderacion() == null || entregable.ponderacion() < 1 || entregable.ponderacion() > 100) {
                        throw new BadRequestException("La ponderacion del entregable '" + entregable.nombre() + "' debe estar entre 1 y 100");
                    }
                    validarFechasEntregableNuevo(entregable, null);
                    sumEntregables += entregable.ponderacion();
                }
                if (sumEntregables != 100) {
                    throw new BadRequestException("La suma de ponderaciones de entregables en el hito '" + hito.nombre() + "' debe ser exactamente 100");
                }
            }
        }
    }

    private void validarFechasEntregableNuevo(EntregableDTO entregable, LocalDate fechaInicioProyecto) {
        if (entregable.fechaInicio() == null) {
            throw new BadRequestException("La fecha de inicio del entregable '" + entregable.nombre() + "' es obligatoria");
        }
        if (entregable.fechaLimite() == null) {
            throw new BadRequestException("La fecha limite del entregable '" + entregable.nombre() + "' es obligatoria");
        }
        if (fechaInicioProyecto != null && entregable.fechaInicio().isBefore(fechaInicioProyecto)) {
            throw new BadRequestException("La fecha de inicio del entregable '" + entregable.nombre() + "' debe ser mayor o igual a la fecha de inicio del proyecto");
        }
        if (entregable.fechaLimite().isBefore(entregable.fechaInicio())) {
            throw new BadRequestException("La fecha limite del entregable '" + entregable.nombre() + "' debe ser mayor o igual a la fecha de inicio del entregable");
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
                resolveDirectorAsignado(p),
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

    private void aplicarEstrategiaPeti(Proyecto proyecto, String estrategiaPeti) {
        EstrategiaPetiConfig config = petiCatalogService.resolveEstrategiaConfig(estrategiaPeti);
        proyecto.setEstrategiaPetiConfig(config);
        proyecto.setEstrategiaPeti(config != null ? parseLegacyEstrategiaPeti(config.getCodigo()) : null);
    }

    private EstrategiaPeti parseLegacyEstrategiaPeti(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        try {
            return EstrategiaPeti.valueOf(codigo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveEstrategiaPetiCodigo(Proyecto proyecto) {
        if (proyecto.getEstrategiaPetiConfig() != null) {
            return proyecto.getEstrategiaPetiConfig().getCodigo();
        }
        return proyecto.getEstrategiaPeti() != null ? proyecto.getEstrategiaPeti().name() : null;
    }

    private ProyectoResponseDTO mapToResponseDto(Proyecto p) {
        return new ProyectoResponseDTO(
                p.getId(),
                p.getId(),
                p.getNombre(),
                p.getDependencia(),
                resolveDirectorAsignado(p),
                resolveDirectorCorreoAsignado(p),
                p.getObjetivoGeneral(),
                p.getObjetivosEspecificos().stream().map(ObjetivoEspecifico::getDescripcion).collect(Collectors.toList()),
                p.getFechaInicio(),
                p.getEstadoCodigo(),
                p.getAvanceTotal(),
                p.getPeti(),
                p.getVigenciaPeti(),
                resolveEstrategiaPetiCodigo(p),
                p.getTienePlanComunicaciones(),
                p.getPatrocinador() != null ? new PatrocinadorDTO(p.getPatrocinador().getNombre(), p.getPatrocinador().getEntidad(), p.getPatrocinador().getCargo(), p.getPatrocinador().getProcesoSigc(), p.getPatrocinador().getProcedimiento()) : null,
                p.getEquipoTrabajo().stream().map(m -> new EquipoTrabajoDTO(m.getNombre(), m.getCargo(), m.getRol())).collect(Collectors.toList()),
                p.getFurag() != null ? new FuragDTO(p.getFurag().getInfraestructuraDatos(), p.getFurag().getInteroperabilidad(), p.getFurag().getDigitalizacionAutomatizacion(), p.getFurag().getContratacionPublica(), p.getFurag().getServiciosNube(), p.getFurag().getSandbox(), p.getFurag().getTecnologiasEmergentes()) : null,
                p.getFases().stream().map(f -> new FaseResponseDTO(
                        f.getId(), f.getNombre(), f.getDescripcion(), f.getPonderacion(), f.getAvanceCalculado(),
                        f.getHitos().stream()
                                .sorted(ProjectHierarchyOrdering.HITOS_BY_SEQUENCE)
                                .map(h -> new HitoResponseDTO(
                                h.getId(), h.getNombre(), h.getDescripcion(), h.getPonderacion(), h.getAvanceCalculado(),
                                h.getEntregables().stream()
                                        .sorted(ProjectHierarchyOrdering.ENTREGABLES_BY_SCHEDULE)
                                        .map(e -> new EntregableResponseDTO(
                                        e.getId(), e.getNombre(), e.getPonderacion(), e.getEstadoCodigo(), e.esConforme(), e.getFechaInicio(), e.getFechaLimite()
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private SeguridadUsuarioProyecto findDirectorAsignado(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return null;
        }
        return usuarioProyectoRepository.findActiveDirectorAssignmentsByProyectoId(proyecto.getId()).stream()
                .filter(assignment -> assignment.getUsuario() != null)
                .findFirst()
                .orElse(null);
    }

    private String resolveDirectorAsignado(Proyecto proyecto) {
        return directorNameFromAssignment(findDirectorAsignado(proyecto));
    }

    private String resolveDirectorCorreoAsignado(Proyecto proyecto) {
        SeguridadUsuarioProyecto assignment = findDirectorAsignado(proyecto);
        return assignment != null && assignment.getUsuario() != null
                ? firstNonBlank(assignment.getUsuario().getCorreo())
                : null;
    }

    private String directorNameFromAssignment(SeguridadUsuarioProyecto assignment) {
        if (assignment == null || assignment.getUsuario() == null) {
            return null;
        }
        return firstNonBlank(
                assignment.getUsuario().getNombre(),
                assignment.getUsuario().getUsername()
        );
    }
}
