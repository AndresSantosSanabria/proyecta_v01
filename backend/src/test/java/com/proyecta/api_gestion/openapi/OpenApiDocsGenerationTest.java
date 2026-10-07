package com.proyecta.api_gestion.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Genera la especificación OpenAPI con las anotaciones nuevas (sin arrancar el
 * servidor en un puerto) y valida que cumple los criterios de la documentación:
 *
 * <ul>
 *   <li>172 operaciones, todas con summary y tags.</li>
 *   <li>Seguridad global bearerAuth + {@code security: []} en endpoints públicos.</li>
 *   <li>Respuestas de error por endpoint según las reglas del
 *       {@code ErrorResponseOperationCustomizer} (400/401/403/404/413/422/429/500).</li>
 *   <li>Esquemas ProblemDetail y UnauthorizedError registrados; cero 503.</li>
 * </ul>
 *
 * <p>Además vuelca el spec completo a {@code api-docs-after.json} en el directorio
 * temporal de trabajo para revisión manual.</p>
 */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class OpenApiDocsGenerationTest {

    private static final Pattern KEBAB_TAG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)+$");
    private static final Set<String> MUTATION_METHODS = Set.of("post", "put", "patch", "delete");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void laEspecificacionOpenApiCumpleLosCriteriosDeDocumentacion() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode spec = MAPPER.readTree(json);

        Path dump = Path.of(System.getProperty("java.io.tmpdir"), "opencode", "api-docs-after.json");
        Files.createDirectories(dump.getParent());
        Files.writeString(dump, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(spec), StandardCharsets.UTF_8);

        List<String> violations = new ArrayList<>();
        StringBuilder report = new StringBuilder();

        // ---- cabecera ----
        String openapiVersion = spec.path("openapi").asText("");
        assertTrue(openapiVersion.startsWith("3."), "openapi version: " + openapiVersion);
        String title = spec.path("info").path("title").asText("");
        assertTrue(title.contains("Proyecta"), "title: " + title);

        JsonNode securitySchemes = spec.path("components").path("securitySchemes");
        if (!securitySchemes.has("bearerAuth")) {
            violations.add("components.securitySchemes falta bearerAuth");
        } else {
            JsonNode bearer = securitySchemes.get("bearerAuth");
            if (!"http".equals(bearer.path("type").asText()) || !"bearer".equals(bearer.path("scheme").asText())) {
                violations.add("bearerAuth no es http/bearer: " + bearer);
            }
        }
        JsonNode globalSecurity = spec.path("security");
        boolean globalBearer = StreamSupport.stream(globalSecurity.spliterator(), false)
                .anyMatch(n -> n.has("bearerAuth"));
        if (!globalBearer) {
            violations.add("security global no contiene bearerAuth: " + globalSecurity);
        }

        JsonNode schemas = spec.path("components").path("schemas");
        for (String required : List.of("ProblemDetail", "UnauthorizedError")) {
            if (!schemas.has(required)) {
                violations.add("components.schemas falta " + required);
            }
        }

        // ---- operaciones ----
        int totalOps = 0;
        int withSummary = 0;
        int ops201 = 0;
        int ops204 = 0;
        int publicOps = 0;
        int noFiveHundred = 0;
        Map<String, Integer> errorFreq = new java.util.TreeMap<>();
        Set<String> tagsSeen = new TreeSet<>();

        JsonNode paths = spec.path("paths");
        Iterator<Map.Entry<String, JsonNode>> pathIt = paths.fields();
        while (pathIt.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIt.next();
            String path = pathEntry.getKey();
            Iterator<Map.Entry<String, JsonNode>> opIt = pathEntry.getValue().fields();
            while (opIt.hasNext()) {
                Map.Entry<String, JsonNode> opEntry = opIt.next();
                String method = opEntry.getKey();
                if (!Set.of("get", "post", "put", "patch", "delete").contains(method)) {
                    continue;
                }
                JsonNode op = opEntry.getValue();
                totalOps++;
                String where = method.toUpperCase() + " " + path;

                // summary y tags
                if (op.path("summary").asText("").isBlank()) {
                    violations.add(where + ": sin summary");
                } else {
                    withSummary++;
                }
                JsonNode tags = op.path("tags");
                if (!tags.isArray() || tags.isEmpty()) {
                    violations.add(where + ": sin tags");
                } else {
                    for (JsonNode tag : tags) {
                        String t = tag.asText();
                        tagsSeen.add(t);
                        if (KEBAB_TAG.matcher(t).matches()) {
                            violations.add(where + ": tag con formato kebab por defecto '" + t + "'");
                        }
                    }
                }

                // respuestas
                JsonNode responses = op.path("responses");
                boolean has2xx = false;
                Iterator<String> codes = responses.fieldNames();
                while (codes.hasNext()) {
                    String code = codes.next();
                    if (code.startsWith("2")) {
                        has2xx = true;
                    }
                    if (code.equals("503")) {
                        violations.add(where + ": declara 503 (eliminado de la API)");
                    }
                    if (code.startsWith("4") || code.startsWith("5")) {
                        errorFreq.merge(code, 1, Integer::sum);
                    }
                }
                if (!has2xx) {
                    violations.add(where + ": sin respuesta 2xx");
                }
                if (!responses.has("500")) {
                    violations.add(where + ": sin 500");
                    noFiveHundred++;
                }
                if (responses.has("201")) {
                    ops201++;
                    if (!method.equals("post")) {
                        violations.add(where + ": 201 en método que no es POST");
                    }
                }
                if (responses.has("204")) {
                    ops204++;
                }

                // inputs / path params / multipart
                boolean hasInputs = op.path("parameters").isArray() && !op.path("parameters").isEmpty()
                        || op.has("requestBody");
                boolean hasPathParam = op.path("parameters").isArray()
                        && StreamSupport.stream(op.path("parameters").spliterator(), false)
                            .anyMatch(p -> "path".equals(p.path("in").asText()));
                boolean multipart = false;
                Iterator<Map.Entry<String, JsonNode>> contentIt =
                        op.path("requestBody").path("content").fields();
                while (contentIt.hasNext()) {
                    if (contentIt.next().getKey().startsWith("multipart/")) {
                        multipart = true;
                    }
                }
                boolean isPublic = path.startsWith("/api/v1/public/");

                if (isPublic) {
                    publicOps++;
                    if (!op.has("security")) {
                        violations.add(where + ": público sin campo security");
                    } else if (!op.get("security").isEmpty()) {
                        violations.add(where + ": público con security no vacío: " + op.get("security"));
                    }
                    for (String code : List.of("404", "429", "500")) {
                        if (!responses.has(code)) {
                            violations.add(where + ": público sin " + code);
                        }
                    }
                    continue;
                }

                for (String code : List.of("401", "403", "500")) {
                    if (!responses.has(code)) {
                        violations.add(where + ": sin " + code);
                    }
                }
                if (hasInputs && !responses.has("400")) {
                    violations.add(where + ": con inputs sin 400");
                }
                if (hasPathParam && !responses.has("404")) {
                    violations.add(where + ": con path param sin 404");
                }
                if (MUTATION_METHODS.contains(method) && !responses.has("422")) {
                    violations.add(where + ": mutación sin 422");
                }
                if (multipart && !responses.has("413")) {
                    violations.add(where + ": multipart sin 413");
                }
            }
        }

        report.append("openapi=").append(openapiVersion)
                .append(" ops=").append(totalOps)
                .append(" conSummary=").append(withSummary)
                .append(" public=").append(publicOps)
                .append(" sin500=").append(noFiveHundred)
                .append(" 201=").append(ops201)
                .append(" 204=").append(ops204)
                .append('\n');
        report.append("tags (").append(tagsSeen.size()).append("): ").append(tagsSeen).append('\n');
        report.append("codigos de error: ").append(errorFreq).append('\n');
        report.append("violaciones: ").append(violations.size()).append('\n');
        violations.forEach(v -> report.append("  - ").append(v).append('\n'));
        Path reportPath = Path.of(System.getProperty("java.io.tmpdir"), "opencode", "openapi-validation.txt");
        Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);

        assertEquals(172, totalOps, "se esperaban 172 operaciones, hay " + totalOps);
        assertTrue(violations.isEmpty(), violations.size() + " violaciones, ver " + reportPath);
    }
}
