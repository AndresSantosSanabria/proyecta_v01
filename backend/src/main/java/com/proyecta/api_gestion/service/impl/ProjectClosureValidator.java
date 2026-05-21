package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProjectClosureValidator {

    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final IStorageProvider storageProvider;

    public ProjectClosureValidator(FaseRepository faseRepository,
                                   HitoRepository hitoRepository,
                                   EntregableRepository entregableRepository,
                                   IStorageProvider storageProvider) {
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.storageProvider = storageProvider;
    }

    public void validarCierre(String proyectoId) {
        List<Fase> fases = faseRepository.findByProyectoId(proyectoId);

        long totalEntregables = 0;
        long entregablesSinArchivo = 0;

        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepository.findByFaseId(fase.getId());
            for (Hito hito : hitos) {
                if (hito.getAvanceCalculado() == null || hito.getAvanceCalculado().compareTo(new BigDecimal("100")) < 0) {
                    throw new BadRequestException("Validación fallida: El hito '" + hito.getNombre() +
                        "' no tiene un cumplimiento del 100% (Actual: " + hito.getAvanceCalculado() + "%).");
                }

                if (!"APROBADO".equalsIgnoreCase(hito.getEstadoRevision())) {
                    throw new BadRequestException("Validación fallida: El hito '" + hito.getNombre() +
                        "' no ha sido APROBADO por el Gestor de Proyectos.");
                }

                List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId());
                for (Entregable entregable : entregables) {
                    totalEntregables++;
                    if (!storageProvider.fileExists("evidencias", entregable.getArchivoPdf())) {
                        entregablesSinArchivo++;
                    }
                }
            }
        }

        if (totalEntregables == 0) {
            throw new BadRequestException("Validación fallida: El proyecto no tiene entregables registrados.");
        }

        if (entregablesSinArchivo > 0) {
            throw new BadRequestException("Validación fallida: Existen " + entregablesSinArchivo + " entregables sin archivo físico cargado.");
        }
    }
}
