package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.application.port.out.persistence.FuragRespuestaRepositoryPort;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.model.Furag;
import com.proyecta.api_gestion.domain.model.FuragRespuesta;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.enums.RespuestaFurag;
import com.proyecta.api_gestion.dto.config.FuragPreguntaDTO;
import com.proyecta.api_gestion.dto.config.FuragPreguntaRespuestaDTO;
import com.proyecta.api_gestion.dto.proyecto.FuragDTO;
import com.proyecta.api_gestion.service.config.PetiCatalogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FuragSupport {

    private static final String FURAG_INFRAESTRUCTURA_DATOS = "infraestructuraDatos";
    private static final String FURAG_INTEROPERABILIDAD = "interoperabilidad";
    private static final String FURAG_DIGITALIZACION_AUTOMATIZACION = "digitalizacionAutomatizacion";
    private static final String FURAG_CONTRATACION_PUBLICA = "contratacionPublica";
    private static final String FURAG_SERVICIOS_NUBE = "serviciosNube";
    private static final String FURAG_SANDBOX = "sandbox";
    private static final String FURAG_TECNOLOGIAS_EMERGENTES = "tecnologiasEmergentes";

    private final PetiCatalogService petiCatalogService;
    private final FuragRespuestaRepositoryPort furagRespuestaRepositoryPort;

    public FuragSupport(PetiCatalogService petiCatalogService,
                        FuragRespuestaRepositoryPort furagRespuestaRepositoryPort) {
        this.petiCatalogService = petiCatalogService;
        this.furagRespuestaRepositoryPort = furagRespuestaRepositoryPort;
    }

    public Furag buildFurag(FuragDTO dto) {
        if (dto == null || dto.respuestas() == null || dto.respuestas().isEmpty()) {
            return null;
        }

        Map<String, RespuestaFurag> respuestas = normalizeFuragResponses(dto.respuestas());
        Furag furag = new Furag();
        furag.setRespuestas(respuestas);
        furag.setInfraestructuraDatos(resolveFuragAnswer(respuestas, FURAG_INFRAESTRUCTURA_DATOS));
        furag.setInteroperabilidad(resolveFuragAnswer(respuestas, FURAG_INTEROPERABILIDAD));
        furag.setDigitalizacionAutomatizacion(resolveFuragAnswer(respuestas, FURAG_DIGITALIZACION_AUTOMATIZACION));
        furag.setContratacionPublica(resolveFuragAnswer(respuestas, FURAG_CONTRATACION_PUBLICA));
        furag.setServiciosNube(resolveFuragAnswer(respuestas, FURAG_SERVICIOS_NUBE));
        furag.setSandbox(resolveFuragAnswer(respuestas, FURAG_SANDBOX));
        furag.setTecnologiasEmergentes(resolveFuragAnswer(respuestas, FURAG_TECNOLOGIAS_EMERGENTES));
        validarFuragCompleto(furag, respuestas);
        return furag;
    }

    private List<FuragPreguntaRespuestaDTO> buildFuragDetalle(Furag furag) {
        Map<String, RespuestaFurag> respuestas = buildFuragRespuestasMap(furag);
        Map<String, String> labels = new LinkedHashMap<>();
        petiCatalogService.getFuragPreguntas().forEach(pregunta -> {
            String normalizedKey = canonicalizeFuragKey(pregunta.key());
            if (normalizedKey != null) {
                labels.put(normalizedKey, pregunta.label());
            }
            if (pregunta.label() != null) {
                labels.putIfAbsent(trimToNull(pregunta.label()), pregunta.label());
            }
        });
        List<FuragPreguntaRespuestaDTO> detalle = new ArrayList<>();
        for (Map.Entry<String, RespuestaFurag> entry : respuestas.entrySet()) {
            String key = canonicalizeFuragKey(entry.getKey());
            String label = labels.getOrDefault(key, entry.getKey());
            String technicalKey = key != null ? key : entry.getKey();
            detalle.add(new FuragPreguntaRespuestaDTO(technicalKey, label, entry.getValue()));
        }
        return detalle;
    }

    private Map<String, RespuestaFurag> normalizeFuragResponses(Map<String, RespuestaFurag> respuestas) {
        Map<String, RespuestaFurag> normalized = new LinkedHashMap<>();
        if (respuestas == null) {
            return normalized;
        }
        for (Map.Entry<String, RespuestaFurag> entry : respuestas.entrySet()) {
            String key = trimToNull(entry.getKey());
            if (key == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(key, entry.getValue());
        }
        return normalized;
    }

    private RespuestaFurag resolveFuragAnswer(Map<String, RespuestaFurag> respuestas, String canonicalKey) {
        if (respuestas == null || respuestas.isEmpty() || canonicalKey == null) {
            return null;
        }
        if (respuestas.containsKey(canonicalKey)) {
            return respuestas.get(canonicalKey);
        }
        String canonicalLabel = petiCatalogService.getFuragPreguntas().stream()
                .filter(pregunta -> canonicalKey.equals(canonicalizeFuragKey(pregunta.key())) || canonicalKey.equals(pregunta.key()))
                .map(FuragPreguntaDTO::label)
                .findFirst()
                .orElse(null);
        if (canonicalLabel != null && respuestas.containsKey(canonicalLabel)) {
            return respuestas.get(canonicalLabel);
        }
        for (Map.Entry<String, RespuestaFurag> entry : respuestas.entrySet()) {
            String key = canonicalizeFuragKey(entry.getKey());
            if (canonicalKey.equals(key)) {
                return entry.getValue();
            }
            if (canonicalLabel != null && canonicalLabel.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String canonicalizeFuragKey(String key) {
        if (key == null) {
            return null;
        }
        String cleaned = key.trim();
        if (cleaned.isBlank()) {
            return null;
        }
        String normalized = cleaned.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        String mapped = mapearFuragKey(normalized);
        return mapped != null ? mapped : cleaned;
    }

    private String mapearFuragKey(String normalized) {
        String infraestructura = mapearFuragInfraestructura(normalized);
        if (infraestructura != null) {
            return infraestructura;
        }
        String procesos = mapearFuragProcesos(normalized);
        if (procesos != null) {
            return procesos;
        }
        return mapearFuragTecnologias(normalized);
    }

    private String mapearFuragInfraestructura(String normalized) {
        if (normalized.equals("infraestructuradedatos") || normalized.equals("usoinfraestructuradedatos")) return FURAG_INFRAESTRUCTURA_DATOS;
        if (normalized.contains("infraestructura") && normalized.contains("dato")) return FURAG_INFRAESTRUCTURA_DATOS;
        if (normalized.contains(FURAG_INTEROPERABILIDAD)) return FURAG_INTEROPERABILIDAD;
        return null;
    }

    private String mapearFuragProcesos(String normalized) {
        if (normalized.equals("digitalizacionautomatizacion") || (normalized.contains("digitalizacion") && normalized.contains("automatizacion"))) return FURAG_DIGITALIZACION_AUTOMATIZACION;
        if (normalized.equals("contratacionpublica") || (normalized.contains("contratacion") && normalized.contains("publica"))) return FURAG_CONTRATACION_PUBLICA;
        if (normalized.equals("serviciosnube") || (normalized.contains("servicios") && normalized.contains("nube"))) return FURAG_SERVICIOS_NUBE;
        return null;
    }

    private String mapearFuragTecnologias(String normalized) {
        if (normalized.equals(FURAG_SANDBOX) || normalized.contains(FURAG_SANDBOX)) return FURAG_SANDBOX;
        if (normalized.equals("tecnologiasemergentes") || (normalized.contains("tecnologias") && normalized.contains("emergentes"))) return FURAG_TECNOLOGIAS_EMERGENTES;
        if (normalized.equals("elpepe")) return "elPepe";
        if (normalized.equals("sanpepe")) return "sanPepe";
        return null;
    }

    private Map<String, RespuestaFurag> buildFuragRespuestasMap(Furag furag) {
        Map<String, RespuestaFurag> map = new LinkedHashMap<>();
        if (furag == null) {
            return map;
        }
        Map<String, RespuestaFurag> dynamic = furag.getRespuestas();
        if (dynamic != null && !dynamic.isEmpty()) {
            map.putAll(normalizeFuragResponses(dynamic));
            return map;
        }
        if (furag.getInfraestructuraDatos() != null) map.put(FURAG_INFRAESTRUCTURA_DATOS, furag.getInfraestructuraDatos());
        if (furag.getInteroperabilidad() != null) map.put(FURAG_INTEROPERABILIDAD, furag.getInteroperabilidad());
        if (furag.getDigitalizacionAutomatizacion() != null) map.put(FURAG_DIGITALIZACION_AUTOMATIZACION, furag.getDigitalizacionAutomatizacion());
        if (furag.getContratacionPublica() != null) map.put(FURAG_CONTRATACION_PUBLICA, furag.getContratacionPublica());
        if (furag.getServiciosNube() != null) map.put(FURAG_SERVICIOS_NUBE, furag.getServiciosNube());
        if (furag.getSandbox() != null) map.put(FURAG_SANDBOX, furag.getSandbox());
        if (furag.getTecnologiasEmergentes() != null) map.put(FURAG_TECNOLOGIAS_EMERGENTES, furag.getTecnologiasEmergentes());
        return map;
    }

    public void validarFuragCompleto(Furag furag, Map<String, RespuestaFurag> respuestas) {
        if (furag == null) {
            throw new BadRequestException("FURAG es obligatorio y no puede ser nulo.");
        }
        List<String> camposFaltantes = new ArrayList<>();
        if (respuestas == null || respuestas.isEmpty()) {
            camposFaltantes.add("respuestas");
        } else {
            for (Map.Entry<String, RespuestaFurag> entry : respuestas.entrySet()) {
                if (entry.getValue() == null) {
                    camposFaltantes.add(entry.getKey());
                }
            }
        }
        if (!camposFaltantes.isEmpty()) {
            throw new BadRequestException("FURAG incompleto. No se pudieron resolver estos campos: " + String.join(", ", camposFaltantes));
        }
    }

    public void sincronizarRespuestasFurag(Proyecto proyecto) {
        if (proyecto == null || proyecto.getId() == null) {
            return;
        }

        Furag furag = proyecto.getFurag();
        if (furag == null) {
            furag = reconstruirFuragDesdeRespuestas(proyecto.getId());
        }

        furagRespuestaRepositoryPort.deleteByProyectoId(proyecto.getId());

        if (furag == null) {
            return;
        }

        Map<String, RespuestaFurag> respuestas = buildFuragRespuestasMap(furag);
        if (respuestas.isEmpty()) {
            return;
        }

        Map<String, String> labels = petiCatalogService.getFuragPreguntas().stream()
                .collect(Collectors.toMap(FuragPreguntaDTO::key, FuragPreguntaDTO::label, (a, b) -> a, LinkedHashMap::new));

        List<FuragRespuesta> items = new ArrayList<>();
        for (Map.Entry<String, RespuestaFurag> entry : respuestas.entrySet()) {
            String pregunta = labels.getOrDefault(canonicalizeFuragKey(entry.getKey()), entry.getKey());
            items.add(buildFuragRespuesta(proyecto, entry.getKey(), pregunta, entry.getValue()));
        }
        furagRespuestaRepositoryPort.saveAll(items);
    }

    private FuragRespuesta buildFuragRespuesta(Proyecto proyecto, String codigo, String pregunta, RespuestaFurag respuesta) {
        FuragRespuesta item = new FuragRespuesta();
        item.setProyecto(proyecto);
        item.setCodigoPregunta(codigo);
        item.setPregunta(pregunta);
        item.setRespuesta(respuesta);
        item.setObligatoria(true);
        return item;
    }

    public Furag reconstruirFuragDesdeRespuestas(String proyectoId) {
        List<FuragRespuesta> respuestas = furagRespuestaRepositoryPort.findByProyectoIdOrderByCodigoPreguntaAsc(proyectoId);
        if (respuestas == null || respuestas.isEmpty()) {
            return null;
        }

        Furag furag = new Furag();
        Map<String, RespuestaFurag> dynamic = new LinkedHashMap<>();
        for (FuragRespuesta respuesta : respuestas) {
            aplicarRespuestaFurag(furag, dynamic, respuesta);
        }

        furag.setRespuestas(dynamic);
        return dynamic.isEmpty() ? null : furag;
    }

    private void aplicarRespuestaFurag(Furag furag, Map<String, RespuestaFurag> dynamic, FuragRespuesta respuesta) {
        if (respuesta == null) {
            return;
        }
        String key = trimToNull(respuesta.getCodigoPregunta());
        if (key == null) {
            key = trimToNull(respuesta.getPregunta());
        }
        if (key == null || respuesta.getRespuesta() == null) {
            return;
        }
        dynamic.put(key, respuesta.getRespuesta());
        String canonicalKey = canonicalizeFuragKey(key);
        if (canonicalKey == null) {
            return;
        }
        switch (canonicalKey) {
            case FURAG_INFRAESTRUCTURA_DATOS -> furag.setInfraestructuraDatos(respuesta.getRespuesta());
            case FURAG_INTEROPERABILIDAD -> furag.setInteroperabilidad(respuesta.getRespuesta());
            case FURAG_DIGITALIZACION_AUTOMATIZACION -> furag.setDigitalizacionAutomatizacion(respuesta.getRespuesta());
            case FURAG_CONTRATACION_PUBLICA -> furag.setContratacionPublica(respuesta.getRespuesta());
            case FURAG_SERVICIOS_NUBE -> furag.setServiciosNube(respuesta.getRespuesta());
            case FURAG_SANDBOX -> furag.setSandbox(respuesta.getRespuesta());
            case FURAG_TECNOLOGIAS_EMERGENTES -> furag.setTecnologiasEmergentes(respuesta.getRespuesta());
            default -> {
                // Preguntas nuevas quedan disponibles en el mapa dinamico.
            }
        }
    }

    public FuragDTO furagDtoDe(Proyecto p) {
        Furag furag = reconstruirFuragDesdeRespuestas(p.getId());
        return furag != null ? buildFuragDto(furag) : null;
    }

    public List<FuragPreguntaRespuestaDTO> furagDetalleDe(Proyecto p) {
        Furag furag = reconstruirFuragDesdeRespuestas(p.getId());
        return furag != null ? buildFuragDetalle(furag) : List.of();
    }

    private FuragDTO buildFuragDto(Furag furag) {
        FuragDTO dto = new FuragDTO(buildFuragRespuestasMap(furag));
        dto.setDetalle(buildFuragDetalle(furag));
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
