package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ProjectClosureRecordDTO;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.closure.ClosureTemplate;
import com.proyecta.api_gestion.domain.model.closure.ProjectClosureRecord;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureTemplateRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ProjectClosureRecordRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectClosureRecordService {

    private final ProjectClosureRecordRepositoryPort recordRepositoryPort;
    private final ClosureTemplateRepositoryPort templateRepositoryPort;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final ObjectMapper objectMapper;

    public ProjectClosureRecordService(ProjectClosureRecordRepositoryPort recordRepositoryPort,
                                       ClosureTemplateRepositoryPort templateRepositoryPort,
                                       ProyectoRepositoryPort proyectoRepositoryPort,
                                       ObjectMapper objectMapper) {
        this.recordRepositoryPort = recordRepositoryPort;
        this.templateRepositoryPort = templateRepositoryPort;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProjectClosureRecordDTO getByProject(String proyectoId) {
        ProjectClosureRecord registro = recordRepositoryPort.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe registro de cierre para el proyecto: " + proyectoId));
        return toDTO(registro);
    }

    @Transactional
    public ProjectClosureRecordDTO saveOrUpdate(String proyectoId, String formDataJson, String username) {
        Proyecto proyecto = proyectoRepositoryPort.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));

        ClosureTemplate template = templateRepositoryPort.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));

        String templateSnapshot = template.getTemplateJson();

        ProjectClosureRecord existing = recordRepositoryPort.findByProyectoId(proyectoId).orElse(null);

        if (existing != null) {
            existing.setFormData(formDataJson);
            existing.setTemplate(template);
            existing.setTemplateSnapshot(templateSnapshot);
            existing.setCreatedBy(username);
            return toDTO(recordRepositoryPort.save(existing));
        }

        ProjectClosureRecord registro = new ProjectClosureRecord();
        registro.setProyecto(proyecto);
        registro.setTemplate(template);
        registro.setTemplateSnapshot(templateSnapshot);
        registro.setFormData(formDataJson);
        registro.setCreatedBy(username);
        return toDTO(recordRepositoryPort.save(registro));
    }

    private ProjectClosureRecordDTO toDTO(ProjectClosureRecord r) {
        Object snapshot;
        Object formData;
        try {
            snapshot = objectMapper.readValue(r.getTemplateSnapshot(), Object.class);
        } catch (JsonProcessingException _) {
            snapshot = r.getTemplateSnapshot();
        }
        try {
            formData = objectMapper.readValue(r.getFormData(), Object.class);
        } catch (JsonProcessingException _) {
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
