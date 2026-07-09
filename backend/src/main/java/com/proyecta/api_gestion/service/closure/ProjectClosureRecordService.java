package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ProjectClosureRecordDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.closure.ClosureTemplate;
import com.proyecta.api_gestion.model.closure.ProjectClosureRecord;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.closure.ClosureTemplateRepository;
import com.proyecta.api_gestion.repository.closure.ProjectClosureRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectClosureRecordService {

    private final ProjectClosureRecordRepository recordRepository;
    private final ClosureTemplateRepository templateRepository;
    private final ProyectoRepository proyectoRepository;
    private final ObjectMapper objectMapper;

    public ProjectClosureRecordService(ProjectClosureRecordRepository recordRepository,
                                       ClosureTemplateRepository templateRepository,
                                       ProyectoRepository proyectoRepository,
                                       ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.templateRepository = templateRepository;
        this.proyectoRepository = proyectoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProjectClosureRecordDTO getByProject(String proyectoId) {
        ProjectClosureRecord record = recordRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe registro de cierre para el proyecto: " + proyectoId));
        return toDTO(record);
    }

    @Transactional
    public ProjectClosureRecord saveOrUpdate(String proyectoId, String formDataJson, String username) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        ClosureTemplate template = templateRepository.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));

        String templateSnapshot = template.getTemplateJson();

        ProjectClosureRecord existing = recordRepository.findByProyectoId(proyectoId).orElse(null);

        if (existing != null) {
            existing.setFormData(formDataJson);
            existing.setTemplate(template);
            existing.setTemplateSnapshot(templateSnapshot);
            existing.setCreatedBy(username);
            return toDTO(recordRepository.save(existing));
        }

        ProjectClosureRecord record = new ProjectClosureRecord();
        record.setProyecto(proyecto);
        record.setTemplate(template);
        record.setTemplateSnapshot(templateSnapshot);
        record.setFormData(formDataJson);
        record.setCreatedBy(username);
        return toDTO(recordRepository.save(record));
    }

    private ProjectClosureRecordDTO toDTO(ProjectClosureRecord r) {
        Object snapshot;
        Object formData;
        try {
            snapshot = objectMapper.readValue(r.getTemplateSnapshot(), Object.class);
        } catch (JsonProcessingException e) {
            snapshot = r.getTemplateSnapshot();
        }
        try {
            formData = objectMapper.readValue(r.getFormData(), Object.class);
        } catch (JsonProcessingException e) {
            formData = r.getFormData();
        }
        return new ProjectClosureRecordDTO(
                r.getId(),
                r.getProyecto() != null ? r.getProyecto().getId() : null,
                r.getTemplate() != null ? r.getTemplate().getId() : null,
                snapshot,
                formData,
                r.getCreatedAt(),
                r.getCreatedBy()
        );
    }
}
