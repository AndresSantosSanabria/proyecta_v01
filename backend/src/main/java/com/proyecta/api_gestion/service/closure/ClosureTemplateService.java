package com.proyecta.api_gestion.service.closure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateDTO;
import com.proyecta.api_gestion.dto.closure.ClosureTemplateRequest;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.closure.ClosureTemplate;
import com.proyecta.api_gestion.application.port.out.persistence.closure.ClosureTemplateRepositoryPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClosureTemplateService {
    private static final String JSON_LABEL = "label";
    private static final String JSON_TIPO_SECCION = "tipo_seccion";
    private static final String JSON_TITULO = "titulo";

    private final ClosureTemplateRepositoryPort repository;
    private final ObjectMapper objectMapper;
    private final TemplateResolver templateResolver;

    /** Proxy transaccional de esta misma bean; null en tests unitarios sin contexto Spring. */
    private final ClosureTemplateService self;

    private ClosureTemplateService selfProxy() {
        return self != null ? self : this;
    }

    public ClosureTemplateService(ClosureTemplateRepositoryPort repository, ObjectMapper objectMapper, TemplateResolver templateResolver,
                                  @Lazy ClosureTemplateService self) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.templateResolver = templateResolver;
        this.self = self;
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
        String templateJson = selfProxy().getActiveTemplateJson();
        return templateResolver.resolveTemplateWithAnswers(templateJson, answerMap);
    }

    public void validarFormData(String templateJson, String formDataJson) {
        try {
            JsonNode template = objectMapper.readTree(templateJson);
            JsonNode data = objectMapper.readTree(formDataJson);
            JsonNode secciones = template.get("secciones");
            if (secciones == null || !secciones.isArray()) return;

            JsonNode fields = data.has("fields") ? data.get("fields") : data;

            for (JsonNode seccion : secciones) {
                validateFormSeccion(seccion, fields);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (JsonProcessingException _) {
            throw new BadRequestException("Error al validar los datos del formulario contra la plantilla.");
        }
    }

    private void validateFormSeccion(JsonNode seccion, JsonNode fields) {
        boolean esFormulario = "formulario".equals(seccion.get(JSON_TIPO_SECCION).asText(""));
        JsonNode campos = seccion.get("campos");
        if (!esFormulario || campos == null || !campos.isArray()) return;
        for (JsonNode campo : campos) {
            validateFormCampo(campo, fields);
        }
    }

    private void validateFormCampo(JsonNode campo, JsonNode fields) {
        if (!campo.has("requerido") || !campo.get("requerido").asBoolean(false)) return;
        String id;
        if (campo.has("id_campo")) {
            id = campo.get("id_campo").asText();
        } else {
            id = campo.has("id") ? campo.get("id").asText() : null;
        }
        if (id == null || id.isBlank()) return;
        JsonNode val = fields.get(id);
        if (!isAnswerBlank(val)) return;
        String label = campo.has(JSON_LABEL) ? campo.get(JSON_LABEL).asText() : id;
        throw new BadRequestException("El campo obligatorio '" + label + "' no tiene respuesta.");
    }

    private boolean isAnswerBlank(JsonNode val) {
        if (val == null || val.isNull()) return true;
        return val.isTextual() && val.asText("").isBlank();
    }

    private void validateTemplateJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode secciones = root.get("secciones");
            if (secciones == null || !secciones.isArray() || secciones.isEmpty()) {
                throw new BadRequestException("La plantilla debe tener al menos una seccion.");
            }
            for (JsonNode seccion : secciones) {
                validateTemplateSeccion(seccion);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (JsonProcessingException _) {
            throw new BadRequestException("El JSON de la plantilla no es valido.");
        }
    }

    private void validateTemplateSeccion(JsonNode seccion) {
        if (!seccion.has(JSON_TITULO) || seccion.get(JSON_TITULO).asText("").isBlank()) {
            throw new BadRequestException("Todas las secciones deben tener un titulo.");
        }
        if (!seccion.has(JSON_TIPO_SECCION)) {
            throw new BadRequestException("Cada seccion debe tener un tipo (formulario o tabla).");
        }
        String tipo = seccion.get(JSON_TIPO_SECCION).asText();
        if ("tabla".equals(tipo)) {
            validateTemplateTabla(seccion);
        }
        if ("formulario".equals(tipo)) {
            validateTemplateFormulario(seccion);
        }
    }

    private void validateTemplateTabla(JsonNode seccion) {
        JsonNode columnas = seccion.get("columnas");
        if (columnas == null || !columnas.isArray() || columnas.isEmpty()) {
            throw new BadRequestException("La seccion '" + seccion.get(JSON_TITULO).asText() + "' tipo tabla debe tener al menos una columna.");
        }
        for (JsonNode col : columnas) {
            if (!col.has(JSON_LABEL) || col.get(JSON_LABEL).asText("").isBlank()) {
                throw new BadRequestException("Todas las columnas de la tabla deben tener un label.");
            }
        }
    }

    private void validateTemplateFormulario(JsonNode seccion) {
        JsonNode campos = seccion.get("campos");
        if (campos == null || !campos.isArray() || campos.isEmpty()) {
            throw new BadRequestException("La seccion '" + seccion.get(JSON_TITULO).asText() + "' tipo formulario debe tener al menos un campo.");
        }
        for (JsonNode campo : campos) {
            if (!campo.has(JSON_LABEL) || campo.get(JSON_LABEL).asText("").isBlank()) {
                throw new BadRequestException("Todos los campos deben tener un label.");
            }
        }
    }

    private String serializeTemplateJson(Object templateJson) {
        if (templateJson instanceof String s) {
            try {
                objectMapper.readTree(s);
                return s;
            } catch (JsonProcessingException _) {
                throw new BadRequestException("El JSON de la plantilla no es valido.");
            }
        }
        try {
            return objectMapper.writeValueAsString(templateJson);
        } catch (JsonProcessingException _) {
            throw new BadRequestException("No fue posible serializar la plantilla.");
        }
    }

    private ClosureTemplateDTO toDTO(ClosureTemplate t) {
        Object json;
        try {
            json = objectMapper.readValue(t.getTemplateJson(), Object.class);
        } catch (JsonProcessingException _) {
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
