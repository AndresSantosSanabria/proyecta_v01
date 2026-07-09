package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateDTO;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateRequest;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.closure.ClosureTemplate;
import com.proyecta.api_gestion.repository.closure.ClosureTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClosureTemplateService {

    private final ClosureTemplateRepository repository;
    private final ObjectMapper objectMapper;
    private final TemplateResolver templateResolver;

    public ClosureTemplateService(ClosureTemplateRepository repository, ObjectMapper objectMapper, TemplateResolver templateResolver) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.templateResolver = templateResolver;
    }

    @Transactional(readOnly = true)
    public ClosureTemplateDTO getActive() {
        ClosureTemplate template = repository.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla de acta de cierre activa."));
        return toDTO(template);
    }

    @Transactional
    public ClosureTemplateDTO save(ClosureTemplateRequest request, String username) {
        String templateJson = serializeTemplateJson(request.templateJson());
        validateTemplateJson(templateJson);

        repository.deactivateAll();

        ClosureTemplate template = new ClosureTemplate();
        template.setCodigoProceso(request.codigoProceso());
        template.setNombreDocumento(request.nombreDocumento());
        template.setActivo(true);
        template.setTemplateJson(templateJson);
        template.setCreatedBy(username);
        template.setUpdatedBy(username);

        int nextVersion = repository.findAll().stream()
                .mapToInt(ClosureTemplate::getVersionNum)
                .max().orElse(0) + 1;
        template.setVersionNum(nextVersion);

        ClosureTemplate saved = repository.save(template);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public String getActiveTemplateJson() {
        ClosureTemplate template = repository.findByActivoTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No hay plantilla activa."));
        return template.getTemplateJson();
    }

    @Transactional(readOnly = true)
    public String getActiveTemplateJsonResolved(java.util.Map<Long, String> answerMap) {
        String templateJson = getActiveTemplateJson();
        return templateResolver.resolveTemplateWithAnswers(templateJson, answerMap);
    }

    private void validateTemplateJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray() || secciones.isEmpty()) {
                throw new BadRequestException("La plantilla debe tener al menos una seccion.");
            }
            for (JsonNode seccion : secciones) {
                if (!seccion.has("titulo") || seccion.get("titulo").asText("").isBlank()) {
                    throw new BadRequestException("Todas las secciones deben tener un titulo.");
                }
                if (!seccion.has("tipo_seccion")) {
                    throw new BadRequestException("Cada seccion debe tener un tipo (formulario o tabla).");
                }
                String tipo = seccion.get("tipo_seccion").asText();
                if ("tabla".equals(tipo)) {
                    JsonNode columnas = seccion.get("columnas");
                    if (columnas == null || !columnas.isArray() || columnas.isEmpty()) {
                        throw new BadRequestException("La seccion '" + seccion.get("titulo").asText() + "' tipo tabla debe tener al menos una columna.");
                    }
                    for (JsonNode col : columnas) {
                        if (!col.has("label") || col.get("label").asText("").isBlank()) {
                            throw new BadRequestException("Todas las columnas de la tabla deben tener un label.");
                        }
                    }
                }
                if ("formulario".equals(tipo)) {
                    JsonNode campos = seccion.get("campos");
                    if (campos == null || !campos.isArray() || campos.isEmpty()) {
                        throw new BadRequestException("La seccion '" + seccion.get("titulo").asText() + "' tipo formulario debe tener al menos un campo.");
                    }
                    for (JsonNode campo : campos) {
                        if (!campo.has("label") || campo.get("label").asText("").isBlank()) {
                            throw new BadRequestException("Todos los campos deben tener un label.");
                        }
                    }
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("El JSON de la plantilla no es valido.");
        }
    }

    private String serializeTemplateJson(Object templateJson) {
        if (templateJson instanceof String s) {
            try {
                objectMapper.readTree(s);
                return s;
            } catch (JsonProcessingException e) {
                throw new BadRequestException("El JSON de la plantilla no es valido.");
            }
        }
        try {
            return objectMapper.writeValueAsString(templateJson);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("No fue posible serializar la plantilla.");
        }
    }

    private ClosureTemplateDTO toDTO(ClosureTemplate t) {
        Object json;
        try {
            json = objectMapper.readValue(t.getTemplateJson(), Object.class);
        } catch (JsonProcessingException e) {
            json = t.getTemplateJson();
        }
        return new ClosureTemplateDTO(
                t.getId(),
                t.getCodigoProceso(),
                t.getVersionNum(),
                t.getNombreDocumento(),
                t.getActivo(),
                json,
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getCreatedBy(),
                t.getUpdatedBy()
        );
    }
}
