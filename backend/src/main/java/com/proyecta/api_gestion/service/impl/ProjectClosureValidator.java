package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.ObjetivoEspecifico;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.FaseRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.HitoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioProyectoRepositoryPort;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectClosureValidator {

    private final FaseRepositoryPort faseRepositoryPort;
    private final HitoRepositoryPort hitoRepositoryPort;
    private final EntregableRepositoryPort entregableRepositoryPort;
    private final IStorageProvider storageProvider;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort;
    private final ProyectoBeneficioImpactoService beneficioImpactoService;

    public ProjectClosureValidator(FaseRepositoryPort faseRepositoryPort,
                                   HitoRepositoryPort hitoRepositoryPort,
                                   EntregableRepositoryPort entregableRepositoryPort,
                                   IStorageProvider storageProvider,
                                   ProyectoRepositoryPort proyectoRepositoryPort,
                                   SeguridadUsuarioProyectoRepositoryPort usuarioProyectoRepositoryPort,
                                   ProyectoBeneficioImpactoService beneficioImpactoService) {
        this.faseRepositoryPort = faseRepositoryPort;
        this.hitoRepositoryPort = hitoRepositoryPort;
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.storageProvider = storageProvider;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.usuarioProyectoRepositoryPort = usuarioProyectoRepositoryPort;
        this.beneficioImpactoService = beneficioImpactoService;
    }

    public void validarCierre(String proyectoId) {
        List<Fase> fases = faseRepositoryPort.findByProyectoId(proyectoId);
        TotalesEntregables totales = contarEntregables(fases);

        if (totales.total() == 0) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene entregables registrados.");
        }

        if (totales.conformes() < totales.total()) {
            throw new BadRequestException("Validacion fallida: todos los entregables deben estar aprobados antes de solicitar el cierre.");
        }

        if (totales.sinArchivo() > 0) {
            throw new BadRequestException("Validacion fallida: existen " + totales.sinArchivo() + " entregables sin archivo fisico cargado.");
        }

        beneficioImpactoService.validarDiligenciado(proyectoId);
    }

    private record TotalesEntregables(long total, long sinArchivo, long conformes) {
    }

    private TotalesEntregables contarEntregables(List<Fase> fases) {
        long totalEntregables = 0;
        long entregablesSinArchivo = 0;
        long entregablesConformes = 0;

        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepositoryPort.findByFaseId(fase.getId());
            for (Hito hito : hitos) {
                List<Entregable> entregables = entregableRepositoryPort.findByHitoId(hito.getId());
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
        return new TotalesEntregables(totalEntregables, entregablesSinArchivo, entregablesConformes);
    }

    public void validarDatosActa(String proyectoId) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new BadRequestException("Proyecto no encontrado para validar acta: " + proyectoId));

        if (proyecto.getFechaInicio() == null) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene fecha de inicio registrada.");
        }

        if (proyecto.getObjetivoGeneral() == null || proyecto.getObjetivoGeneral().isBlank()) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene objetivo general registrado.");
        }

        List<ObjetivoEspecifico> objetivos = proyecto.getObjetivosEspecificos();
        if (objetivos != null) {
            objetivos.removeIf(obj -> obj == null || obj.getDescripcion() == null || obj.getDescripcion().isBlank());
        }

        if (proyecto.getPatrocinador() == null
                || isBlank(proyecto.getPatrocinador().getNombre())
                || isBlank(proyecto.getPatrocinador().getCargo())
                || isBlank(proyecto.getPatrocinador().getEntidad())) {
            throw new BadRequestException("Validacion fallida: el proyecto no tiene patrocinador completo para el acta.");
        }

        var directorAsignado = usuarioProyectoRepositoryPort
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
