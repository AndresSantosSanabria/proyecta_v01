package com.proyecta.api_gestion.config.openapi;

import com.proyecta.api_gestion.dto.common.ProblemDetailDTO;
import com.proyecta.api_gestion.dto.common.UnauthorizedErrorDTO;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationComponentsCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documenta automáticamente las respuestas de error de <b>cada</b> endpoint según
 * HTTP y el comportamiento real del backend, sin repetir anotaciones en ~170 métodos.
 *
 * <p>Reglas de aplicabilidad:</p>
 * <ul>
 *   <li><b>400</b> solo si la operación acepta entradas (query/path/header o body).</li>
 *   <li><b>401 / 403</b> solo en endpoints autenticados.</li>
 *   <li><b>404</b> solo si la ruta tiene variables ({@code /{id}}) o es un endpoint
 *       público con firma (el filtro responde 404 genérico).</li>
 *   <li><b>422</b> solo en operaciones de escritura (POST/PUT/PATCH/DELETE), donde
 *       los servicios lanzan {@code UnprocessableEntityException}.</li>
 *   <li><b>500</b> siempre (existe handler catch-all).</li>
 *   <li><b>429</b> solo en endpoints públicos (rate limit por IP).</li>
 * </ul>
 *
 * <p>Nunca pisa códigos ya declarados con {@code @ApiResponse}/{@link StandardApiResponses}.
 * Registra además los esquemas {@code ProblemDetail} y {@code UnauthorizedError} para que
 * las referencias {@code $ref} apunten a componentes existentes. Los endpoints marcados con
 * {@link PublicEndpoint} quedan sin seguridad JWT ({@code security: []}).</p>
 */
@Component
public class ErrorResponseOperationCustomizer implements GlobalOperationComponentsCustomizer, OpenApiCustomizer {

    private static final String SCHEMA_PROBLEM = "#/components/schemas/ProblemDetail";
    private static final String SCHEMA_UNAUTHORIZED = "#/components/schemas/UnauthorizedError";
    private static final String MEDIA_JSON = "application/json";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        return customize(operation, null, handlerMethod);
    }

    @Override
    public Operation customize(Operation operation, Components components, HandlerMethod handlerMethod) {
        if (components != null) {
            registerSchemas(components);
        }

        ApiResponses responses = operation.getResponses() != null
                ? operation.getResponses()
                : new ApiResponses();

        boolean publicEndpoint = isPublicEndpoint(handlerMethod);
        RequestMapping mapping = findMapping(handlerMethod);

        if (publicEndpoint) {
            operation.setSecurity(List.of());
            if (hasInputs(operation)) {
                putProblem(responses, "400", DashboardSwaggerConstants.RESPONSE_400_DESC,
                        DashboardSwaggerConstants.EXAMPLE_400);
            }
            putPublicNotFound(responses);
            putMessageOnly(responses, "429", DashboardSwaggerConstants.RESPONSE_429_DESC,
                    DashboardSwaggerConstants.EXAMPLE_429);
            putProblem(responses, "500", DashboardSwaggerConstants.RESPONSE_500_DESC,
                    DashboardSwaggerConstants.EXAMPLE_500);
        } else {
            if (hasInputs(operation)) {
                putProblem(responses, "400", DashboardSwaggerConstants.RESPONSE_400_DESC,
                        DashboardSwaggerConstants.EXAMPLE_400);
            }
            putUnauthorized(responses);
            putProblem(responses, "403", DashboardSwaggerConstants.RESPONSE_403_DESC,
                    DashboardSwaggerConstants.EXAMPLE_403);
            if (hasPathParam(operation)) {
                putProblem(responses, "404", DashboardSwaggerConstants.RESPONSE_404_DESC,
                        DashboardSwaggerConstants.EXAMPLE_404);
            }
            if (isMutation(mapping, operation)) {
                putProblem(responses, "422", DashboardSwaggerConstants.RESPONSE_422_DESC,
                        DashboardSwaggerConstants.EXAMPLE_422);
            }
            if (isUpload(mapping, handlerMethod)) {
                putProblem(responses, "413", DashboardSwaggerConstants.RESPONSE_413_DESC,
                        DashboardSwaggerConstants.EXAMPLE_413);
            }
            putProblem(responses, "500", DashboardSwaggerConstants.RESPONSE_500_DESC,
                    DashboardSwaggerConstants.EXAMPLE_500);
        }

        operation.setResponses(responses);
        return operation;
    }

    /**
     * Red de seguridad: garantiza que los componentes de error existan en la especificación
     * aunque ninguna anotación {@code @ApiResponse} los referencie.
     */
    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        registerSchemas(openApi.getComponents());
    }

    private void registerSchemas(Components components) {
        registerSchema(components, ProblemDetailDTO.class);
        registerSchema(components, UnauthorizedErrorDTO.class);
    }

    private void registerSchema(Components components, Class<?> type) {
        var schemas = ModelConverters.getInstance().readAll(type);
        if (schemas.isEmpty()) {
            return;
        }
        if (components.getSchemas() == null) {
            components.schemas(new LinkedHashMap<>());
        }
        schemas.forEach((name, schema) -> components.getSchemas().putIfAbsent(name, schema));
    }

    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PublicEndpoint.class) != null
                || AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PublicEndpoint.class) != null;
    }

    private boolean hasInputs(Operation operation) {
        if (operation.getRequestBody() != null) {
            return true;
        }
        return operation.getParameters() != null && !operation.getParameters().isEmpty();
    }

    private boolean hasPathParam(Operation operation) {
        if (operation.getParameters() == null) {
            return false;
        }
        return operation.getParameters().stream().anyMatch(p -> "path".equals(p.getIn()));
    }

    /**
     * Mapping de la operación. Si la anotación solo está declarada en el método de la
     * interfaz (patrón de los controladores con interfaz), se busca también ahí.
     */
    private RequestMapping findMapping(HandlerMethod handlerMethod) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequestMapping.class);
        if (mapping != null) {
            return mapping;
        }
        Class<?> beanType = handlerMethod.getBeanType();
        for (Class<?> itf : beanType.getInterfaces()) {
            try {
                Method interfaceMethod = itf.getMethod(
                        handlerMethod.getMethod().getName(), handlerMethod.getMethod().getParameterTypes());
                mapping = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, RequestMapping.class);
                if (mapping != null) {
                    return mapping;
                }
            } catch (NoSuchMethodException _) {
                // el método no viene de esta interfaz
            }
        }
        return null;
    }

    private boolean isMutation(RequestMapping mapping, Operation operation) {
        if (mapping == null) {
            return false;
        }
        if (mapping.method().length == 0) {
            return operation.getRequestBody() != null;
        }
        for (RequestMethod method : mapping.method()) {
            switch (method) {
                case POST, PUT, PATCH, DELETE -> {
                    return true;
                }
                default -> {
                    // GET/HEAD/OPTIONS: no aplica 422
                }
            }
        }
        return false;
    }

    private boolean isUpload(RequestMapping mapping, HandlerMethod handlerMethod) {
        if (mapping != null) {
            for (String consumes : mapping.consumes()) {
                if (consumes.startsWith("multipart/")) {
                    return true;
                }
            }
        }
        for (Class<?> parameterType : handlerMethod.getMethod().getParameterTypes()) {
            if (org.springframework.web.multipart.MultipartFile.class.isAssignableFrom(parameterType)) {
                return true;
            }
        }
        return false;
    }

    private void putProblem(ApiResponses responses, String code, String description, String exampleJson) {
        if (responses.containsKey(code)) {
            return;
        }
        responses.put(code, jsonResponse(description, SCHEMA_PROBLEM, Map.of("Ejemplo", exampleJson)));
    }

    private void putUnauthorized(ApiResponses responses) {
        if (responses.containsKey("401")) {
            return;
        }
        responses.put("401", jsonResponse(DashboardSwaggerConstants.RESPONSE_401_DESC,
                SCHEMA_UNAUTHORIZED, Map.of("401 Unauthorized", DashboardSwaggerConstants.EXAMPLE_401)));
    }

    /**
     * 404 de endpoints públicos: el filtro de firma responde genérico y, si la firma es
     * válida, el handler puede devolver ProblemDetail. Se documenta con esquema libre.
     */
    private void putPublicNotFound(ApiResponses responses) {
        if (responses.containsKey("404")) {
            return;
        }
        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("Firma inválida o expirada", DashboardSwaggerConstants.EXAMPLE_404_PUBLIC);
        examples.put("Recurso inexistente", DashboardSwaggerConstants.EXAMPLE_404);
        responses.put("404", jsonResponse(DashboardSwaggerConstants.RESPONSE_404_DESC, null, examples));
    }

    private void putMessageOnly(ApiResponses responses, String code, String description, String exampleJson) {
        if (responses.containsKey(code)) {
            return;
        }
        responses.put(code, jsonResponse(description, null, Map.of("Rate limit", exampleJson)));
    }

    /**
     * Construye una respuesta JSON. Si {@code schemaRef} es {@code null} usa un objeto
     * libre (para cuerpos que no siguen el Problem Details).
     */
    private ApiResponse jsonResponse(String description, String schemaRef, Map<String, String> examples) {
        Schema<Object> schema = new Schema<>();
        if (schemaRef != null) {
            schema.set$ref(schemaRef);
        } else {
            schema.type("object");
        }

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);
        examples.forEach((name, json) -> mediaType.addExamples(name, new Example().value(json)));

        Content content = new Content();
        content.addMediaType(MEDIA_JSON, mediaType);

        return new ApiResponse().description(description).content(content);
    }
}
