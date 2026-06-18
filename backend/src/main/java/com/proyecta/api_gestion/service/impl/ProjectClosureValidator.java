package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectClosureValidator {

    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final IStorageProvider storageProvider;
    private final ProyectoRepository proyectoRepository;
    private final SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    private final ProyectoBeneficioImpactoService beneficioImpactoService;

    public ProjectClosureValidator(FaseRepository faseRepository,
                                   HitoRepository hitoRepository,
                                   EntregableRepository entregableRepository,
                                   IStorageProvider storageProvider,
                                   ProyectoRepository proyectoRepository,
                                   SeguridadUsuarioProyectoRepository usuarioProyectoRepository,
                                   ProyectoBeneficioImpactoService beneficioImpactoService) {
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.storageProvider = storageProvider;
        this.proyectoRepository = proyectoRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.beneficioImpactoService = beneficioImpactoService;
    }

    public void validarCierre(String proyectoId) {
        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);

        long totalEntregables = 0;
        long entregablesSinArchivo = 0;
        long entregablesConformes = 0;

        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepository.findByFaseId(fase.getId());
            for (Hito hito : hitos) {
                List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId());
                for (Entregable entregable : entregables) {
                    totalEntregables++;
                    if (!storageProvider.fileExists("evidencias", entregable.getArchivoPdf())) {
                        entregablesSinArchivo++;
                    }
                    if (entregable.esConforme()) {
                        entregablesConformes++;
                    }
                }
            }
        }

        if (totalEntregables == 0) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene entregables registrados.");
        }

        if (entregablesConformes < totalEntregables) {
            throw new BadRequestException("Validacion fallida: todos los entregables deben estar aprobados antes de solicitar el cierre.");
        }

        if (entregablesSinArchivo > 0) {
            throw new BadRequestException("Validacion fallida: existen " + entregablesSinArchivo + " entregables sin archivo fisico cargado.");
        }

        beneficioImpactoService.validarDiligenciado(proyectoId);
    }

    public void validarDatosActa(String proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new BadRequestException("Proyecto no encontrado para validar acta: " + proyectoId));

        if (proyecto.getFechaInicio() == null) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene fecha de inicio registrada.");
        }

        if (proyecto.getObjetivoGeneral() == null || proyecto.getObjetivoGeneral().isBlank()) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene objetivo general registrado.");
        }

        List<ObjetivoEspecifico> objetivos = proyecto.getObjetivosEspecificos();
        if (objetivos == null || objetivos.stream().noneMatch(obj -> obj != null && obj.getDescripcion() != null && !obj.getDescripcion().isBlank())) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene objetivos especificos registrados.");
        }

        if (proyecto.getPatrocinador() == null
                || isBlank(proyecto.getPatrocinador().getNombre())
                || isBlank(proyecto.getPatrocinador().getCargo())
                || isBlank(proyecto.getPatrocinador().getEntidad())) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene patrocinador completo para el acta.");
        }

        var directorAsignado = usuarioProyectoRepository
                .findActiveDirectorAssignmentsByProyectoId(proyectoId)
                .stream()
                .findFirst()
                .orElse(null);

        if (directorAsignado == null || directorAsignado.getUsuario() == null) {
            throw new BadRequestException("Validacion fallida: no existe un director asignado para este proyecto.");
        }

        if (isBlank(directorAsignado.getUsuario().getNombre())) {
            throw new BadRequestException("Validacion fallida: el director asignado no tiene nombre registrado.");
        }

        if (isBlank(directorAsignado.getCargo())) {
            throw new BadRequestException("Validacion fallida: el director asignado no tiene cargo registrado.");
        }

        String entidad = directorAsignado.getUsuario().getDependencia();
        if (isBlank(entidad) && isBlank(proyecto.getDependencia())) {
            throw new BadRequestException("Validacion fallida: el director asignado no tiene entidad o dependencia registrada.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
