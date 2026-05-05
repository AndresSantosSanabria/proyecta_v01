package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.dto.cronograma.CronogramaResponseDTO;
import com.proyecta.api_gestion.dto.cronograma.CronogramaUploadResponseDTO;
import com.proyecta.api_gestion.dto.cronograma.FaseGanttDTO;
import com.proyecta.api_gestion.dto.cronograma.HitoGanttDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.FaseRepository;
import com.proyecta.api_gestion.repository.HitoRepository;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.interfaces.CronogramaService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;

@Service
public class CronogramaServiceImpl implements CronogramaService {

    private final ProyectoRepository proyectoRepository;
    private final FaseRepository faseRepository;
    private final HitoRepository hitoRepository;
    private final EntregableRepository entregableRepository;
    private final FileStorageServiceImpl fileStorageService;

    public CronogramaServiceImpl(ProyectoRepository proyectoRepository,
                                 FaseRepository faseRepository,
                                 HitoRepository hitoRepository,
                                 EntregableRepository entregableRepository,
                                 FileStorageServiceImpl fileStorageService) {
        this.proyectoRepository = proyectoRepository;
        this.faseRepository = faseRepository;
        this.hitoRepository = hitoRepository;
        this.entregableRepository = entregableRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public CronogramaResponseDTO obtenerCronograma(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        List<Fase> fases = faseRepository.findByProyectoId(projectId);
        List<FaseGanttDTO> vistaGantt = new ArrayList<>();
        
        int totalHitos = 0;

        for (Fase fase : fases) {
            List<Hito> hitos = hitoRepository.findByFaseId(fase.getId());
            hitos.sort(Comparator.comparing(Hito::getNombre));
            
            List<HitoGanttDTO> hitosGantt = new ArrayList<>();

            for (Hito hito : hitos) {
                totalHitos++;
                List<Entregable> entregables = entregableRepository.findByHitoId(hito.getId());
                
                LocalDate hitoStart = null;
                LocalDate hitoEnd = null;
                
                if (!entregables.isEmpty()) {
                    hitoStart = entregables.stream()
                            .map(Entregable::getFechaLimite)
                            .filter(Objects::nonNull)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                    
                    hitoEnd = entregables.stream()
                            .map(Entregable::getFechaLimite)
                            .filter(Objects::nonNull)
                            .max(LocalDate::compareTo)
                            .orElse(null);
                }
                
                hitosGantt.add(new HitoGanttDTO(
                        hito.getId(),
                        hito.getNombre(),
                        hitoStart,
                        hitoEnd
                ));
            }
            
            vistaGantt.add(new FaseGanttDTO(
                    fase.getId(),
                    fase.getNombre(),
                    hitosGantt
            ));
        }

        vistaGantt.sort(Comparator.comparing(FaseGanttDTO::nombre));

        String nombreArchivo = null;
        if (proyecto.getCronogramaPdf() != null) {
            int lastSlash = proyecto.getCronogramaPdf().lastIndexOf("/");
            int lastBackslash = proyecto.getCronogramaPdf().lastIndexOf("\\");
            int slashIndex = Math.max(lastSlash, lastBackslash);
            nombreArchivo = (slashIndex >= 0) ? proyecto.getCronogramaPdf().substring(slashIndex + 1) : proyecto.getCronogramaPdf();
        }

        return new CronogramaResponseDTO(
                proyecto.getId(),
                nombreArchivo,
                LocalDate.now(), // La fecha de carga no se guarda por defecto en Proyecto, retornamos la actual
                proyecto.getDirector(),
                fases.size(),
                totalHitos,
                proyecto.getAvanceTotal(),
                proyecto.getFechaInicio(),
                "/api/v1/proyectos/" + proyecto.getId() + "/cronograma/descargar",
                vistaGantt
        );
    }

    @Override
    @Transactional
    public CronogramaUploadResponseDTO cargarCronograma(String projectId, MultipartFile file) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (EstadoProyecto.CERRADO.equals(proyecto.getEstado())) {
            throw new ForbiddenException("No se puede subir el cronograma a un proyecto cerrado.");
        }

        if (file.isEmpty() || !Objects.equals(file.getContentType(), "application/pdf")) {
            throw new BadRequestException("El archivo no es un PDF válido.");
        }

        if (file.getSize() > 20 * 1024 * 1024) { // 20MB limit as per spec
            throw new BadRequestException("El archivo supera el tamaño permitido de 20MB.");
        }

        String fileName = "cronograma_" + projectId;
        String storedPath = fileStorageService.storeFile(file, fileName);

        proyecto.setCronogramaPdf(storedPath);
        proyectoRepository.save(proyecto);

        return new CronogramaUploadResponseDTO(
                proyecto.getId(),
                file.getOriginalFilename(),
                LocalDate.now(),
                "/api/v1/proyectos/" + proyecto.getId() + "/cronograma/descargar"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarCronograma(String projectId) {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        if (proyecto.getCronogramaPdf() == null) {
            throw new ResourceNotFoundException("No se ha cargado un cronograma para este proyecto.");
        }

        return fileStorageService.loadFileAsResource(proyecto.getCronogramaPdf());
    }
}
